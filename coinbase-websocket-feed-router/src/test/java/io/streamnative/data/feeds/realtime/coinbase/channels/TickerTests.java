package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TickerTests extends AbstractChannelTest {

    private static final String EXPECTED = "{\"type\":\"ticker\",\"sequence\":1111111,\"product_id\":\"ETH-USD\",\"price\":9499.99,\"open_24h\":0.0,\"volume_24h\":525091.56,\"low_24h\":9488.76,\"high_24h\":9540.12,\"volume_30d\":0.0,\"best_bid\":9500.01,\"best_bid_size\":72.55,\"best_ask\":9500.0,\"best_ask_size\":34.56,\"side\":\"SELL\",\"time\":\"2024-03-28T21:32:46.123000Z\",\"millis\":1711661566123,\"trade_id\":2222222,\"last_size\":90.87}";

    private static final String AS_JSON = "{\"type\":\"ticker\",\"sequence\":1111111,\"product_id\":\"ETH-USD\",\"price\":9499.99,\"open_24h\":0.0,\"volume_24h\":525091.56,\"low_24h\":9488.76,\"high_24h\":9540.12,\"volume_30d\":0.0,\"best_bid\":9500.01,\"best_bid_size\":72.55,\"best_ask\":9500.0,\"best_ask_size\":34.56,\"side\":\"SELL\",\"time\":\"2024-03-28T21:32:46.123000Z\",\"trade_id\":2222222,\"last_size\":90.87}";

    private static final Ticker TICKER = new Ticker();

    static {
        TICKER.setBest_ask(9500.0f);
        TICKER.setBest_bid(9500.01f);
        TICKER.setBest_ask_size(34.56f);
        TICKER.setLast_size(90.87f);
        TICKER.setBest_bid_size(72.55f);
        TICKER.setHigh_24h(9540.12f);
        TICKER.setLow_24h(9488.76f);
        TICKER.setPrice(9499.99f);
        TICKER.setSide("SELL");
        TICKER.setSequence(1111111L);
        TICKER.setType("ticker");
        TICKER.setProduct_id("ETH-USD");
        TICKER.setTrade_id(2222222L);
        TICKER.setVolume_24h(525091.54214680004f);

        LocalDateTime time = LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000);
        TICKER.setTime(time);
    }

    @Test
    public void serializeTest() throws JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(TICKER);
        assertNotNull(json);
        assertEquals(EXPECTED, json);
    }

    @Override
    @Test
    public void deserializeTest() throws JsonProcessingException {
        Ticker ticker = getObjectMapper().readValue(AS_JSON, Ticker.class);

        assertEquals(LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000), ticker.getTime());
        assertEquals(1711661566123L, ticker.getMillis());
        assertEquals(TICKER, ticker);
    }
}
