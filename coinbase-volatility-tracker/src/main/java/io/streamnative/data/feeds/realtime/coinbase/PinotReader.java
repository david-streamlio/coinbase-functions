package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PinotReader implements Closeable {

    public static final String URL_PROPERTY_NAME = "db.jdbc.url";
    public static final String QUERY_PROPERTY_NAME = "db.query";

    private static Logger LOG;

    private final String query;

    private final String jdbcUrl;

    private Connection connection;

    private ScheduledExecutorService scheduler;

    private PushSource pushSource;

    public PinotReader(PushSource pushSrc, SourceContext srcCtx) {
        this.pushSource = pushSrc;
        this.LOG = srcCtx.getLogger();

        this.jdbcUrl = (String) srcCtx.getSourceConfig().getConfigs()
                .getOrDefault(URL_PROPERTY_NAME, "jdbc:pinot://localhost:9000?brokers=localhost:8099");

        this.query = (String) srcCtx.getSourceConfig().getConfigs()
                .get(QUERY_PROPERTY_NAME);

        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                executeQuery();
            } catch (SQLException e) {
                e.printStackTrace();
                LOG.error("Unable to execute query", e);
            }
        }, 0, 1, TimeUnit.MINUTES); // Run immediately and then every 1 minute

        // Shut down the scheduler gracefully when the program exits
        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));
    }

    private void executeQuery() throws SQLException {
        LOG.info(String.format("Executing query [%s]", this.query));
        try (Connection connection = getConnection()) {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(this.query);
            while (rs.next()) {
                Volatility volatility = new Volatility(rs.getString("product_id"),
                        Double.valueOf(rs.getDouble("window_start_time")).longValue(),
                        Double.valueOf(rs.getDouble("window_end_time")).longValue(),
                        rs.getDouble("volatility"));
                LOG.info("Publishing " + volatility);
                this.pushSource.consume(new VolatiltyRecord(volatility));
            }

            rs.close();
            stmt.close();
        } catch (Throwable t) {
            LOG.error("Error retrieving data from Pinot ", t);
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(this.jdbcUrl);
    }

    @Override
    public void close() throws IOException {

    }
}
