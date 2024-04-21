package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AuctionTests extends AbstractChannelTest {

    private static final String EXPECTED = "{\"type\":\"auction\",\"product_id\":\"LTC-USD\",\"sequence\":3262786978,\"auction_state\":\"collection\",\"best_bid_price\":333.98,\"best_bid_size\":4.3908825,\"best_ask_price\":333.99,\"best_ask_size\":25.235428,\"open_price\":333.99,\"open_size\":0.193,\"can_open\":\"yes\",\"timestamp\":\"2024-03-28T21:32:46.123000Z\",\"millis\":1711661566123}";

    private static final String AS_JSON = "{\"type\":\"auction\",\"product_id\":\"LTC-USD\",\"sequence\":3262786978,\"auction_state\":\"collection\",\"best_bid_price\":333.98,\"best_bid_size\":4.3908825,\"best_ask_price\":333.99,\"best_ask_size\":25.235428,\"open_price\":333.99,\"open_size\":0.193,\"can_open\":\"yes\",\"timestamp\":\"2024-03-28T21:32:46.123000Z\"}";

    private static final Auction AUCTION = new Auction();

    static {
        AUCTION.setAuction_state("collection");
        AUCTION.setBest_ask_size(25.23542881f);
        AUCTION.setType("auction");
        AUCTION.setBest_bid_size(4.39088265f);
        AUCTION.setSequence(3262786978L);
        AUCTION.setProduct_id("LTC-USD");
        AUCTION.setBest_bid_price(333.98f);
        AUCTION.setBest_ask_price(333.99f);
        AUCTION.setOpen_price(333.99f);
        AUCTION.setOpen_size(0.193f);
        AUCTION.setCan_open("yes");

        LocalDateTime time = LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000);
        AUCTION.setTimestamp(time);
    }

    @Override
    @Test
    public void serializeTest() throws JsonProcessingException {
        String json = getObjectMapper().writeValueAsString(AUCTION);
        assertNotNull(json);
        assertEquals(EXPECTED, json);
    }

    @Override
    @Test
    public void deserializeTest() throws JsonProcessingException {
        Auction auction = getObjectMapper().readValue(EXPECTED, Auction.class);

        assertEquals(LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000), auction.getTimestamp());
        assertEquals(1711661566123L, auction.getMillis());
        assertEquals(AUCTION, auction);
    }
}
