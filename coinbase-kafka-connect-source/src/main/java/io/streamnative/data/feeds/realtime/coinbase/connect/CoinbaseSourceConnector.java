package io.streamnative.data.feeds.realtime.coinbase.connect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

public class CoinbaseSourceConnector extends SourceConnector {

    private Map<String, String> props;

    @Override
    public String version() {
        return "1.1.0";
    }

    @Override
    public void start(Map<String, String> props) {
        this.props = props;
        // Validate config eagerly so misconfiguration fails at Connect REST POST time
        // rather than later in task start.
        new CoinbaseConnectorConfig(props);
    }

    @Override
    public Class<? extends Task> taskClass() {
        return CoinbaseSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // Coinbase WebSocket is one socket per process. Running more than one task
        // would open duplicate sockets and emit each record N times — pin to a single
        // task regardless of what the worker requested.
        List<Map<String, String>> taskConfigs = new ArrayList<>(1);
        taskConfigs.add(new HashMap<>(props));
        return taskConfigs;
    }

    @Override
    public void stop() {
        // No connector-level resources — task owns the WebSocket lifecycle.
    }

    @Override
    public ConfigDef config() {
        return CoinbaseConnectorConfig.CONFIG_DEF;
    }
}
