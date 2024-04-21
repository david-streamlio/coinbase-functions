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

public class WebsocketFeedRouter implements Function<String, Void> {

    private static final String RFQ_TOPIC = "persistent://public/default/coinbase-rfq";
    private static final String TICKER_TOPIC = "persistent://public/default/coinbase-ticker";
    private static final String AUCTION_TOPIC = "persistent://public/default/coinbase-auction";
    private ObjectMapper objectMapper;
    private Logger LOG;

    @Override
    public Void process(String jsonString, Context ctx) throws Exception {
        String feedName = ctx.getCurrentRecord().getKey().orElse("UNKNOWN");

        try {
            if (feedName.equalsIgnoreCase("rfq_match")) {
                RfqMatch match = getObjectMapper().readValue(jsonString, RfqMatch.class);
                LOG.info(String.format("Sending [%s]", match));
                ctx.newOutputMessage(RFQ_TOPIC, Schema.JSON(RfqMatch.class))
                        .value(match)
                        .send();
            } else if (feedName.equalsIgnoreCase("ticker")) {
                ctx.newOutputMessage(TICKER_TOPIC, Schema.JSON(Ticker.class))
                        .value(getObjectMapper().readValue(jsonString, Ticker.class))
                        .send();
            } else if (feedName.equalsIgnoreCase("auction")) {
                ctx.newOutputMessage(AUCTION_TOPIC, Schema.JSON(Auction.class))
                        .value(getObjectMapper().readValue(jsonString, Auction.class))
                        .send();
            }
        } catch (final Exception jmEx) {
            LOG.error(String.format("Unable to process [%s] due to [%s]", jsonString, jmEx.getLocalizedMessage()), jmEx);
            jmEx.printStackTrace();
        }

        return null;
    }

    @Override
    public void initialize(Context ctx) throws Exception {
        Function.super.initialize(ctx);
        this.LOG = ctx.getLogger();
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
