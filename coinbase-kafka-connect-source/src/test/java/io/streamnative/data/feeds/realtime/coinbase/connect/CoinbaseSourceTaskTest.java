package io.streamnative.data.feeds.realtime.coinbase.connect;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CoinbaseSourceTaskTest {

    private CoinbaseSourceTask task;

    @Before
    public void setUp() {
        // Use a private start path that wires config without actually opening the WS.
        // start() in production opens the socket; for unit tests we just need config.
        task = new CoinbaseSourceTask() {
            @Override
            public void start(Map<String, String> props) {
                try {
                    java.lang.reflect.Field cfgField = CoinbaseSourceTask.class.getDeclaredField("config");
                    cfgField.setAccessible(true);
                    cfgField.set(this, new CoinbaseConnectorConfig(props));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        task.start(Map.of(
            CoinbaseConnectorConfig.KAFKA_TOPIC, "coinbase-ticker-feed-kafka",
            CoinbaseConnectorConfig.COINBASE_CHANNELS, "ticker",
            CoinbaseConnectorConfig.COINBASE_PRODUCTS, "BTC-USD,ETH-USD"
        ));
    }

    @Test
    public void tickerFrameProducesKeyedRecord() {
        String json = "{\"type\":\"ticker\",\"sequence\":12345,\"product_id\":\"BTC-USD\","
                    + "\"price\":\"79462.23\",\"time\":\"2026-05-14T00:00:00Z\"}";

        Optional<SourceRecord> rec = task.toRecord(json);
        assertTrue("ticker frame should produce a record", rec.isPresent());

        SourceRecord r = rec.get();
        assertEquals("coinbase-ticker-feed-kafka", r.topic());
        assertEquals(Schema.STRING_SCHEMA, r.keySchema());
        // Base symbol — BTC-USD → BTC. Must match WebsocketFeedRouter.baseSymbol() so
        // downstream selectors see identical keys regardless of ingestion path.
        assertEquals("BTC", r.key());
        // Value is the raw JSON forwarded verbatim.
        assertEquals(json, r.value());
        // type=ticker header for parity with the Pulsar router output.
        assertNotNull(r.headers().lastWithName("type"));
        assertEquals("ticker", r.headers().lastWithName("type").value());
    }

    @Test
    public void heartbeatFrameIsDropped() {
        // Coinbase emits heartbeats on the same socket. type != "ticker" → drop.
        String json = "{\"type\":\"heartbeat\",\"sequence\":1,\"product_id\":\"BTC-USD\"}";
        Optional<SourceRecord> rec = task.toRecord(json);
        assertFalse(rec.isPresent());
    }

    @Test
    public void frameWithoutProductIdIsDropped() {
        // Subscriptions ack messages have type=subscriptions but no product_id.
        String json = "{\"type\":\"subscriptions\",\"channels\":[]}";
        Optional<SourceRecord> rec = task.toRecord(json);
        assertFalse(rec.isPresent());
    }

    @Test
    public void baseSymbolHandlesEdgeCases() {
        assertEquals("BTC",     CoinbaseSourceTask.baseSymbol("BTC-USD"));
        assertEquals("ETH",     CoinbaseSourceTask.baseSymbol("ETH-EUR"));
        assertEquals("NOPAIR",  CoinbaseSourceTask.baseSymbol("NOPAIR"));
        assertEquals("UNKNOWN", CoinbaseSourceTask.baseSymbol(null));
        assertEquals("UNKNOWN", CoinbaseSourceTask.baseSymbol(""));
    }

    @Test
    public void subscribeMessageSerializes() {
        String msg = CoinbaseSourceTask.buildSubscribeMessage(
            List.of("ticker"), List.of("BTC-USD", "ETH-USD"));
        assertTrue(msg.contains("\"type\":\"subscribe\""));
        assertTrue(msg.contains("\"channels\":[\"ticker\"]"));
        assertTrue(msg.contains("\"product_ids\":[\"BTC-USD\",\"ETH-USD\"]"));
    }
}
