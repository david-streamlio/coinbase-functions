package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RfqMatchTests extends AbstractChannelTest {

    private static final String EXPECTED =  "{\"maker_order_id\":\"maker\",\"taker_order_id\":\"taker\",\"side\":\"sell\",\"size\":12.345,\"price\":678.9,\"product_id\":\"Acme Rollerskates\",\"time\":\"2024-03-28T21:32:46.123000Z\",\"millis\":1711661566123}";

    private static final String AS_JSON = "{\"maker_order_id\":\"maker\",\"taker_order_id\":\"taker\",\"side\":\"sell\",\"size\":12.345,\"price\":678.9,\"product_id\":\"Acme Rollerskates\",\"time\":\"2024-03-28T21:32:46.123000Z\"}";

    private static final RfqMatch MATCH = new RfqMatch();

    static {
        MATCH.setSize(12.345d);
        MATCH.setPrice(678.90d);
        MATCH.setSide("sell");
        MATCH.setTime(LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000));
        MATCH.setProduct_id("Acme Rollerskates");
        MATCH.setMaker_order_id("maker");
        MATCH.setTaker_order_id("taker");
    }

    @Test
    public void serializeTest() throws JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(MATCH);
        assertNotNull(json);
        assertEquals(EXPECTED, json);
    }

    @Test
    public void deserializeTest() throws JsonProcessingException {
        RfqMatch match = getObjectMapper().readValue(AS_JSON, RfqMatch.class);
        assertNotNull(match);
        assertEquals(LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000), match.getTime());
        assertEquals(1711661566123L, match.getMillis());
        assertEquals(MATCH, match);
    }
}
