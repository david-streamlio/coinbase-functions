package io.streamnative.data.feeds.realtime.coinbase;

import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Record;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class WebsocketFeedRouterTests {

    private static final HashMap<String, String> TOPIC_MAP = new HashMap<String, String>();

    private WebsocketFeedRouter router = new WebsocketFeedRouter();

    @Mock
    private Context mockContext;

    private Record mockRecord;

    static {
        TOPIC_MAP.put("rfq_match", "persistent://feeds/realtime/rfq-match");
        TOPIC_MAP.put("ticker", "persistent://feeds/realtime/ticker");
    }

    @Test
    public void rfqMatchTest() throws Exception {
        // product_id has the standard BASE-QUOTE form so we also verify the base-symbol
        // extraction (BTC-USD → BTC). The router parses the JSON just to read product_id
        // for the message key; the output body is the original jsonString verbatim under
        // Schema.STRING.
        String json = "{\"maker_order_id\":\"maker\",\"taker_order_id\":\"taker\",\"side\":\"sell\",\"size\":12.345,\"price\":678.9,\"product_id\":\"BTC-USD\",\"time\":\"2024-03-28T21:32:46.123000Z\"}";

        mockContext = mock(Context.class);
        mockRecord = mock(Record.class);
        Logger mockLogger = mock(Logger.class);
        TypedMessageBuilder mockMessageBuilder = mock(TypedMessageBuilder.class);
        MessageId mockMessageId = mock(MessageId.class);

        when(mockContext.getCurrentRecord()).thenReturn(mockRecord);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        when(mockContext.getUserConfigValue(anyString())).thenReturn(Optional.of(TOPIC_MAP));
        when(mockContext.newOutputMessage(anyString(),
                any(Schema.class))).thenReturn(mockMessageBuilder);

        when(mockMessageBuilder.key(anyString())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.value(anyString())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.send()).thenReturn(mockMessageId);
        when(mockRecord.getKey()).thenReturn(Optional.of("rfq_match"));

        router.initialize(mockContext);
        router.process(json, mockContext);

        // Routing decision: rfq_match → mapped destination topic, Schema.STRING.
        verify(mockContext).newOutputMessage("persistent://feeds/realtime/rfq-match", Schema.STRING);
        // Key is the base symbol — "BTC" extracted from "BTC-USD".
        verify(mockMessageBuilder).key("BTC");
        // Body is the original jsonString unchanged.
        verify(mockMessageBuilder).value(json);
        verify(mockMessageBuilder).send();
    }

    @Test
    public void tickerByPropertyTest() throws Exception {
        // coinbase-live-feed publishes the channel as a Pulsar message property `type`
        // (not the message key). Verify the router picks the destination from the
        // property and emits the symbol-keyed output.
        String json = "{\"sequence\":1281102714,\"product_id\":\"ETH-USD\",\"price\":\"3500.00\",\"open_24h\":\"3400.00\",\"volume_24h\":\"100\",\"low_24h\":\"3399\",\"high_24h\":\"3501\",\"volume_30d\":\"200\",\"best_bid\":\"3500\",\"best_bid_size\":\"1\",\"best_ask\":\"3500.01\",\"best_ask_size\":\"1\",\"side\":\"buy\",\"time\":\"2026-05-14T00:23:58.558245Z\",\"trade_id\":1017793859,\"last_size\":\"0.004\"}";

        mockContext = mock(Context.class);
        mockRecord = mock(Record.class);
        Logger mockLogger = mock(Logger.class);
        TypedMessageBuilder mockMessageBuilder = mock(TypedMessageBuilder.class);
        MessageId mockMessageId = mock(MessageId.class);

        Map<String, String> props = new HashMap<>();
        props.put("type", "ticker");

        when(mockContext.getCurrentRecord()).thenReturn(mockRecord);
        when(mockContext.getLogger()).thenReturn(mockLogger);
        when(mockContext.getUserConfigValue(anyString())).thenReturn(Optional.of(TOPIC_MAP));
        when(mockContext.newOutputMessage(anyString(),
                any(Schema.class))).thenReturn(mockMessageBuilder);

        when(mockMessageBuilder.key(anyString())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.value(anyString())).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.send()).thenReturn(mockMessageId);
        // No key set on the incoming record — only the `type` property carries the channel.
        when(mockRecord.getKey()).thenReturn(Optional.empty());
        when(mockRecord.getProperties()).thenReturn(props);

        router.initialize(mockContext);
        router.process(json, mockContext);

        verify(mockContext).newOutputMessage("persistent://feeds/realtime/ticker", Schema.STRING);
        verify(mockMessageBuilder).key("ETH");
        verify(mockMessageBuilder).value(json);
        verify(mockMessageBuilder).send();
    }
}
