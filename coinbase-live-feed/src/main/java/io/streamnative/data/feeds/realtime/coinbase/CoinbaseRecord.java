package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.functions.api.Record;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CoinbaseRecord implements Record<String> {

    private final String key;
    private final String value;

    private final Map<String, String> properties;


    public CoinbaseRecord(String json, String type, String product) {
        this.value = json;
        this.key = type;
        this.properties = new HashMap<>();

        if (StringUtils.isNotBlank(product)) {
            this.properties.put("product", product);
        }
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }
}
