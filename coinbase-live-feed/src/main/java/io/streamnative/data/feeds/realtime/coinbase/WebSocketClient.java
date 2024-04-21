package io.streamnative.data.feeds.realtime.coinbase;

import javax.websocket.*;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.pulsar.io.core.PushSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClientEndpoint
public class WebSocketClient {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);

    private PushSource pushSource;

    private String subscribeMsg;

    private Gson gson = new Gson();

    public WebSocketClient(PushSource pushSource, String subscribeMsg) {
        this.pushSource = pushSource;
        this.subscribeMsg = subscribeMsg;
    }

    @OnOpen
    public void onOpen(Session session) {
        LOG.info("Connected to endpoint: " + session.getBasicRemote());
        try {
            LOG.info("Sending message to endpoint: " + subscribeMsg);
            session.getBasicRemote().sendText(subscribeMsg);
        } catch (IOException ex) {
            LOG.error("Failed to subscribe to Coinbase Websocket endpoint");
        }
    }

    @OnMessage
    public void processMessage(String msg) {
        LOG.info("Received message in client: " + msg);
        JsonObject body = gson.fromJson(msg, JsonObject.class);

        // Use the "type" field as the key
        String type = body.get("type") != null ? body.get("type").getAsString() : "";

        // Add the product_id to the properties for content-based-routing
        String product_id = body.get("product_id") != null ? body.get("product_id").getAsString() : null;
        body.remove("type");
        this.pushSource.consume(new CoinbaseRecord(body.toString(), type, product_id));
    }

    @OnError
    public void processError(Throwable t) {
        LOG.error("", t);
    }
}
