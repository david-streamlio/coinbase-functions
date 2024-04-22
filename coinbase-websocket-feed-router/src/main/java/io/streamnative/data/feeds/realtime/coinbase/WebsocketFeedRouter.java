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

            if (feedName.equalsIgnoreCase("rfq_match")) {
                RfqMatch match = getObjectMapper().readValue(jsonString, RfqMatch.class);
                LOG.info(String.format("Sending [%s] to %s", match, destTopic));
                ctx.newOutputMessage(destTopic, Schema.JSON(RfqMatch.class))
                        .value(match)
                        .send();
            } else if (feedName.equalsIgnoreCase("ticker")) {
                Ticker ticker = getObjectMapper().readValue(jsonString, Ticker.class);
                LOG.info(String.format("Sending [%s] to %s", ticker, destTopic));
                ctx.newOutputMessage(destTopic, Schema.JSON(Ticker.class))
                        .value(ticker)
                        .send();
            } else if (feedName.equalsIgnoreCase("auction")) {
                Auction auction = getObjectMapper().readValue(jsonString, Auction.class);
                LOG.info(String.format("Sending [%s] to %s", auction, destTopic));
                ctx.newOutputMessage(destTopic, Schema.JSON(Auction.class))
                        .value(auction)
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
