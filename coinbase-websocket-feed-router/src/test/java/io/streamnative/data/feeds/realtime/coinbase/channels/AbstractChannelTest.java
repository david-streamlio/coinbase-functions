package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public abstract class AbstractChannelTest {

    public abstract void serializeTest() throws JsonProcessingException;

    public abstract void deserializeTest() throws JsonProcessingException;

    private ObjectMapper objectMapper;

    protected ObjectMapper getObjectMapper() {
        if (this.objectMapper == null) {
            this.objectMapper = new ObjectMapper();
            this.objectMapper.registerModule(new JavaTimeModule());
        }
        return this.objectMapper;
    }
}
