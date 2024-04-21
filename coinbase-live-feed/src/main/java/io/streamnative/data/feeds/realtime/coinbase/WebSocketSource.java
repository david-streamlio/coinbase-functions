package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.io.core.PushSource;
import org.apache.pulsar.io.core.SourceContext;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketSource extends PushSource<String> {

    public final static String WEBSOCKET_URI_PROPERTY = "websocketURI";

    public final static String SUBSCRIPTION_PROPERTY = "subscription";

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketSource.class);

    private WebSocketContainer container;

    private WebSocketClient webSocketClient;

    @Override
    public void open(Map<String, Object> config, SourceContext srcCtx) throws Exception {
        try {
            container = ContainerProvider.getWebSocketContainer();
            webSocketClient = new WebSocketClient(this,
                    srcCtx.getSourceConfig().getConfigs().get(SUBSCRIPTION_PROPERTY).toString());
            String uri = srcCtx.getSourceConfig().getConfigs().get(WEBSOCKET_URI_PROPERTY).toString();
            LOG.info("Connecting to " + uri);
            container.connectToServer(webSocketClient, URI.create(uri));
        } catch (DeploymentException | IOException ex) {
            LOG.error("Unable to connect to Websocket", ex);
        }
    }

    @Override
    public void close() throws Exception {
        //
    }

}
