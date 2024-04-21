package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;
import org.slf4j.Logger;

import java.util.Map;

public class VolatilityTracker extends PushSource<Volatility> {

    private static Logger LOG;

    private PinotReader client;

    @Override
    public void open(Map<String, Object> config, SourceContext srcCtx) throws Exception {
        LOG = srcCtx.getLogger();

        LOG.info("Opening Apache Pinot Source.....");
        client = new PinotReader(this, srcCtx);
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}
