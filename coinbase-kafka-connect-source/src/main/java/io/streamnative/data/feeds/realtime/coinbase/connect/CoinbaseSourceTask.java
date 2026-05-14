package io.streamnative.data.feeds.realtime.coinbase.connect;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.header.ConnectHeaders;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoinbaseSourceTask extends SourceTask {

    private static final Logger LOG = LoggerFactory.getLogger(CoinbaseSourceTask.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Drops frames if the worker stalls — at 40 msg/s and a 200ms poll interval the
     *  steady-state backlog is ~8 records, so 10k headroom is generous. */
    private static final int QUEUE_CAPACITY = 10_000;

    private CoinbaseConnectorConfig config;
    private LinkedBlockingQueue<String> queue;
    private WebSocket ws;
    private HttpClient httpClient;
    /** Accumulates partial text frames — Coinbase emits batched JSON occasionally. */
    private final StringBuilder partial = new StringBuilder();

    @Override
    public String version() {
        return "1.1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.config = new CoinbaseConnectorConfig(props);
        this.queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.httpClient = HttpClient.newHttpClient();
        connectWebSocket();
    }

    private void connectWebSocket() {
        String subscribe = buildSubscribeMessage(config.channels(), config.products());
        LOG.info("Connecting to Coinbase WS at {} with subscribe={}", config.wsUrl(), subscribe);

        CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
            .buildAsync(URI.create(config.wsUrl()), new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    webSocket.sendText(subscribe, true);
                    webSocket.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    partial.append(data);
                    if (last) {
                        String frame = partial.toString();
                        partial.setLength(0);
                        if (!queue.offer(frame)) {
                            LOG.warn("WS queue full ({} cap); dropping frame: {}",
                                QUEUE_CAPACITY, frame.length() > 80 ? frame.substring(0, 80) + "…" : frame);
                        }
                    }
                    webSocket.request(1);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    LOG.error("Coinbase WS error", error);
                }
            });

        this.ws = wsFuture.join();
    }

    /** Build the Coinbase WS subscribe JSON message. Mirrors what coinbase-live-feed sends. */
    static String buildSubscribeMessage(List<String> channels, List<String> products) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("type", "subscribe");
        msg.put("product_ids", products);
        msg.put("channels", channels);
        try {
            return MAPPER.writeValueAsString(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize subscribe message", e);
        }
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        List<SourceRecord> batch = new ArrayList<>();

        // Block briefly for the first frame so we don't busy-spin when the WS is idle…
        String first = queue.poll(config.pollTimeoutMs(), TimeUnit.MILLISECONDS);
        if (first == null) {
            return batch;
        }
        toRecord(first).ifPresent(batch::add);

        // …then drain whatever else is buffered without blocking.
        String next;
        while ((next = queue.poll()) != null) {
            toRecord(next).ifPresent(batch::add);
        }
        return batch;
    }

    /**
     * Translate one Coinbase JSON frame into a Kafka {@link SourceRecord}. Drops frames
     * whose {@code type} isn't in the configured channel list (Coinbase mixes
     * subscriptions, heartbeats, and channel data on one socket), and frames missing
     * a {@code product_id} (heartbeats, subscription acks).
     */
    java.util.Optional<SourceRecord> toRecord(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            String type = root.path("type").asText("");
            if (!config.channels().contains(type)) {
                LOG.debug("Skipping non-data frame type=[{}]", type);
                return java.util.Optional.empty();
            }
            String productId = root.path("product_id").asText(null);
            if (productId == null || productId.isEmpty()) {
                return java.util.Optional.empty();
            }
            String key = baseSymbol(productId);

            ConnectHeaders headers = new ConnectHeaders();
            headers.addString("type", type);

            return java.util.Optional.of(new SourceRecord(
                /* sourcePartition */ Map.of("ws", config.wsUrl()),
                /* sourceOffset    */ Map.of("seq", root.path("sequence").asLong(System.currentTimeMillis())),
                /* topic           */ config.kafkaTopic(),
                /* partition       */ null,
                /* keySchema       */ Schema.STRING_SCHEMA,
                /* key             */ key,
                /* valueSchema     */ Schema.STRING_SCHEMA,
                /* value           */ json,
                /* timestamp       */ null,
                /* headers         */ headers
            ));
        } catch (Exception e) {
            LOG.warn("Failed to parse Coinbase frame; dropping: {}", e.getMessage());
            return java.util.Optional.empty();
        }
    }

    /** Coinbase product_ids are BASE-QUOTE pairs (BTC-USD → BTC). Mirrors
     *  {@code WebsocketFeedRouter.baseSymbol} so downstream selectors that read keys
     *  emitted by either ingestion path see identical values. */
    static String baseSymbol(String productId) {
        if (productId == null || productId.isEmpty()) {
            return "UNKNOWN";
        }
        int dash = productId.indexOf('-');
        return dash < 0 ? productId : productId.substring(0, dash);
    }

    @Override
    public void stop() {
        if (ws != null) {
            try {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "task stop");
            } catch (Exception e) {
                LOG.warn("Error closing WebSocket on stop", e);
            }
        }
    }
}
