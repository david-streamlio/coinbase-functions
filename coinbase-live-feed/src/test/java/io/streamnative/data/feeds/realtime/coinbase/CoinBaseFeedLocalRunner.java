package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.common.functions.FunctionConfig;
import org.apache.pulsar.common.io.SourceConfig;
import org.apache.pulsar.functions.LocalRunner;

import java.util.HashMap;
import java.util.Map;

public class CoinBaseFeedLocalRunner {

    private static final Map<String, Object> CONFIGS = new HashMap<>();

    static {
        CONFIGS.put(WebSocketSource.WEBSOCKET_URI_PROPERTY, "wss://ws-feed.exchange.coinbase.com");
        CONFIGS.put(WebSocketSource.SUBSCRIPTION_PROPERTY, "\"{ \"type\": \"subscribe\", \"channels\": [\n" +
                "{ \"name\": \"heartbeat\", \"product_ids\": [\"ETH-EUR\"]}]}\"");
    }

    public static void main(String[] args) throws Exception {

        SourceConfig sourceConfig =
                SourceConfig.builder()
                        .className(WebSocketSource.class.getName())
                        .configs(CONFIGS)
                        .name("coinbase-feed")
                        .topicName("persistent://public/default/coinbase")
                        .processingGuarantees(FunctionConfig.ProcessingGuarantees.ATMOST_ONCE)
                        .schemaType("string")
                        .build();

        LocalRunner localRunner =
                LocalRunner.builder()
                        .brokerServiceUrl("pulsar://192.168.1.120:6650")
                        .sourceConfig(sourceConfig)
                        .build();

        localRunner.start(false);
        Thread.sleep(120 * 1000);
        localRunner.stop();
    }
}
