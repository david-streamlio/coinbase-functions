package io.streamnative.data.feeds.realtime.coinbase.connect;

import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

public class CoinbaseConnectorConfig extends AbstractConfig {

    public static final String COINBASE_WS_URL = "coinbase.ws.url";
    public static final String COINBASE_CHANNELS = "coinbase.channels";
    public static final String COINBASE_PRODUCTS = "coinbase.products";
    public static final String KAFKA_TOPIC = "kafka.topic";
    public static final String POLL_TIMEOUT_MS = "poll.timeout.ms";

    public static final ConfigDef CONFIG_DEF = new ConfigDef()
        .define(COINBASE_WS_URL, Type.STRING, "wss://ws-feed.exchange.coinbase.com",
            Importance.HIGH, "Coinbase WebSocket feed URL")
        .define(COINBASE_CHANNELS, Type.LIST, List.of("ticker"),
            Importance.HIGH, "Coinbase channels to subscribe to (ticker, auction, rfq_match)")
        .define(COINBASE_PRODUCTS, Type.LIST,
            List.of("BTC-USD","ETH-USD","SOL-USD","DOT-USD","XRP-USD","ADA-USD"),
            Importance.HIGH, "Coinbase product_ids to subscribe to")
        .define(KAFKA_TOPIC, Type.STRING, "coinbase-ticker-feed-kafka",
            Importance.HIGH, "Output Kafka topic — must match the topic the broker-side selector-filter consumes from")
        .define(POLL_TIMEOUT_MS, Type.LONG, 200L,
            Importance.LOW, "Maximum time poll() waits for new records before returning an empty batch");

    public CoinbaseConnectorConfig(Map<String, String> originals) {
        super(CONFIG_DEF, originals);
    }

    public String wsUrl() { return getString(COINBASE_WS_URL); }
    public List<String> channels() { return getList(COINBASE_CHANNELS); }
    public List<String> products() { return getList(COINBASE_PRODUCTS); }
    public String kafkaTopic() { return getString(KAFKA_TOPIC); }
    public long pollTimeoutMs() { return getLong(POLL_TIMEOUT_MS); }
}
