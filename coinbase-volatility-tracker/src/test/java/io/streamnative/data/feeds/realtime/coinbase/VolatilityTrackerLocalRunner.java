package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.common.functions.FunctionConfig;
import org.apache.pulsar.common.io.SourceConfig;
import org.apache.pulsar.functions.LocalRunner;

import java.util.HashMap;
import java.util.Map;

public class VolatilityTrackerLocalRunner {

    private static final Map<String, Object> CONFIGS = new HashMap<>();
    private static final String QUERY = "SELECT product_id,\n" +
            "  FLOOR(millis / (1 * 60 * 1000)) * (1 * 60 * 1000) AS window_start_time,\n" +
            "  FLOOR(millis / (1 * 60 * 1000)) * (1 * 60 * 1000) + (1 * 60 * 1000) AS window_end_time,\n" +
            "  STDDEV_SAMP(price) AS volatility\n" +
            "FROM DefaultTenant.coinbase_ticker\n" +
            "WHERE millis >= FLOOR((NOW() - 300000) / (1 * 60 * 1000)) * (1 * 60 * 1000)\n" +
            "GROUP BY product_id,\n" +
            "  window_start_time\n" +
            "ORDER BY window_start_time ASC,\n" +
            "  volatility DESC LIMIT 10000;";


    static {
        CONFIGS.put(PinotReader.QUERY_PROPERTY_NAME, QUERY);
    }

    public static void main(String[] args) throws Exception {
        SourceConfig sourceConfig =
                SourceConfig.builder()
                        .className(VolatilityTracker.class.getName())
                        .configs(CONFIGS)
                        .topicName("persistent://public/default/volatility")
                        .processingGuarantees(FunctionConfig.ProcessingGuarantees.ATMOST_ONCE)
                        .schemaType(Schema.JSON(Volatility.class).getSchemaInfo().getName())
                        .name("volatility-tracker")
                        .build();

        LocalRunner localRunner =
                LocalRunner.builder()
                        // .brokerServiceUrl("localhost:6650")
                        .sourceConfig(sourceConfig)
                        .build();

        localRunner.start(false);
        Thread.sleep(240 * 1000);
        localRunner.stop();
    }
}
