package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
public class Auction {
    private String type;
    private String product_id;
    private long sequence;
    private String auction_state;
    private float best_bid_price;
    private float best_bid_size;
    private float best_ask_price;
    private float best_ask_size;
    private float open_price;
    private float open_size;
    private String can_open;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    private LocalDateTime timestamp;

    private long millis;

    public long getMillis() {
        Instant instant = timestamp.toInstant(ZoneOffset.UTC);
        return instant.toEpochMilli();
    }
}
