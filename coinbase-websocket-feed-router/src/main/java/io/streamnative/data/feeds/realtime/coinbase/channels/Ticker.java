package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
public class Ticker {
    private String type;
    private long sequence;
    private String product_id;
    private float price;
    private float open_24h;
    private float volume_24h;
    private float low_24h;
    private float high_24h;
    private double volume_30d;
    private float best_bid;
    private float best_bid_size;
    private float best_ask;
    private float best_ask_size;
    private String side;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    private LocalDateTime time;

    private long millis;

    public long getMillis() {
        Instant instant = time.toInstant(ZoneOffset.UTC);
        return instant.toEpochMilli();
    }

    private long trade_id;
    private float last_size;
}
