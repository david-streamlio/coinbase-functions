package io.streamnative.data.feeds.realtime.coinbase.channels;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Data
public class RfqMatch {
   private String maker_order_id;
   private String taker_order_id;
   private String side;
   private double size;
   private double price;
   private String product_id;

   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
   private LocalDateTime time;

   private long millis;

   public long getMillis() {
       Instant instant = time.toInstant(ZoneOffset.UTC);
       return instant.toEpochMilli();
   }
}
