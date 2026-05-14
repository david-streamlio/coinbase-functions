package io.streamnative.data.feeds.realtime.coinbase;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.streamnative.data.feeds.realtime.coinbase.channels.Auction;
import io.streamnative.data.feeds.realtime.coinbase.channels.RfqMatch;
import io.streamnative.data.feeds.realtime.coinbase.channels.Ticker;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import java.util.Map;

public class WebsocketFeedRouter implements Function<String, Void> {

    private Map<String, String> topicMap;

    private ObjectMapper objectMapper;
    private Logger LOG;

    @Override
    public Void process(String jsonString, Context ctx) throws Exception {
        String feedName = ctx.getCurrentRecord().getKey().orElse("UNKNOWN");

        try {
            String destTopic = this.topicMap.get(feedName);
            if (destTopic == null) {
                LOG.debug(String.format("No destination topic mapped for feed [%s]; dropping", feedName));
                return null;
            }

            // Parse just enough to extract product_id for the message key. Output is the
            // original jsonString verbatim with Schema.STRING — matches the source
            // connector's schemaType: STRING and lets downstream broker-side filters
            // (which read Schema<byte[]>) forward the raw bytes through to their KoP
            // consumers without schema-translation surprises.
            String key;
            if (feedName.equalsIgnoreCase("rfq_match")) {
                RfqMatch match = getObjectMapper().readValue(jsonString, RfqMatch.class);
                key = baseSymbol(match.getProduct_id());
                LOG.info(String.format("Sending rfq_match [%s] to %s (key=%s)", match, destTopic, key));
            } else if (feedName.equalsIgnoreCase("ticker")) {
                Ticker ticker = getObjectMapper().readValue(jsonString, Ticker.class);
                key = baseSymbol(ticker.getProduct_id());
                LOG.info(String.format("Sending ticker [%s] to %s (key=%s)", ticker, destTopic, key));
            } else if (feedName.equalsIgnoreCase("auction")) {
                Auction auction = getObjectMapper().readValue(jsonString, Auction.class);
                key = baseSymbol(auction.getProduct_id());
                LOG.info(String.format("Sending auction [%s] to %s (key=%s)", auction, destTopic, key));
            } else {
                LOG.debug(String.format("Unhandled feed type [%s]; dropping", feedName));
                return null;
            }

            ctx.newOutputMessage(destTopic, Schema.STRING)
                    .key(key)
                    .value(jsonString)
                    .send();
        } catch (final Exception jmEx) {
            LOG.error(String.format("Unable to process [%s] due to [%s]", jsonString, jmEx.getLocalizedMessage()), jmEx);
            jmEx.printStackTrace();
        }

        return null;
    }

    /**
     * Coinbase product_ids are BASE-QUOTE pairs (e.g. {@code BTC-USD}, {@code ETH-EUR}).
     * Downstream broker-side filters use the base symbol as the message key so watchlists
     * like {@code __key__ IN ('BTC','ETH')} work without quote-currency boilerplate.
     * Returns the input unchanged if no dash is found, or {@code null}/{@code "UNKNOWN"} for
     * a {@code null} input.
     */
    static String baseSymbol(String productId) {
        if (productId == null || productId.isEmpty()) {
            return "UNKNOWN";
        }
        int dash = productId.indexOf('-');
        return dash < 0 ? productId : productId.substring(0, dash);
    }

    @Override
    public void initialize(Context ctx) throws Exception {
        Function.super.initialize(ctx);
        this.LOG = ctx.getLogger();
        this.topicMap = (Map<String, String>) ctx.getUserConfigValue("TopicMap").get();
        if (topicMap == null) {
            throw new RuntimeException("Invalid configuration");
        }
    }

    @Override
    public void close() throws Exception {
        Function.super.close();
    }

    private ObjectMapper getObjectMapper() {
        if (this.objectMapper == null) {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.registerModule(new JavaTimeModule());
        }
        return this.objectMapper;
    }
}
