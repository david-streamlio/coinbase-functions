package io.streamnative.data.feeds.realtime.coinbase;

import io.streamnative.data.feeds.realtime.coinbase.channels.RfqMatch;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.TypedMessageBuilder;
import org.apache.pulsar.functions.api.Context;
import org.apache.pulsar.functions.api.Record;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class WebsocketFeedRouterTests {

    private static final HashMap<String, String> TOPIC_MAP = new HashMap<String, String>();

    private WebsocketFeedRouter router = new WebsocketFeedRouter();

    @Mock
    private Context mockContext;

    private Record mockRecord;

    static {
        TOPIC_MAP.put("rfq_match", "persistent://feeds/realtime/rfq-match");
    }

    @Test
    public void rfqMatchTest() throws Exception {
        String json = "{\"maker_order_id\":\"maker\",\"taker_order_id\":\"taker\",\"side\":\"sell\",\"size\":12.345,\"price\":678.9,\"product_id\":\"Acme Rollerskates\",\"time\":\"2024-03-28T21:32:46.123000Z\"}";

        RfqMatch match = new RfqMatch();
        match.setSize(12.345d);
        match.setPrice(678.90d);
        match.setSide("sell");
        match.setTime(LocalDateTime.of(2024, 3, 28, 21, 32, 46, 123000000));
        match.setProduct_id("Acme Rollerskates");
        match.setMaker_order_id("maker");
        match.setTaker_order_id("taker");


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

        when(mockMessageBuilder.value(any(RfqMatch.class))).thenReturn(mockMessageBuilder);
        when(mockMessageBuilder.send()).thenReturn(mockMessageId);
        when(mockRecord.getKey()).thenReturn(Optional.of("rfq_match"));

        router.initialize(mockContext);
        router.process(json, mockContext);
        verify(mockMessageBuilder).value(match);
    }
}
