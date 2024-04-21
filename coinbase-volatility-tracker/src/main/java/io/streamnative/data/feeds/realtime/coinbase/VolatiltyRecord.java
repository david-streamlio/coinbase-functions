package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.functions.api.Record;

public class VolatiltyRecord implements Record<Volatility> {

    private final Volatility value;

    public VolatiltyRecord(Volatility volatility) {
        this.value = volatility;
    }

    @Override
    public Volatility getValue() {
        return value;
    }
}
