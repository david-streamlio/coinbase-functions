# Coinbase Realtime Market Data Source

Uses publicly available Market Data API from Coinbase to consume data from a public websocket and publish it to
a Pulsar topic.

This Source Connector can consume from one or more of the Exchange WebSocket Channels;
- [Heartbeat Channel](https://docs.cloud.coinbase.com/exchange/docs/websocket-channels#heartbeat-channel)
- [Status Channel](https://docs.cloud.coinbase.com/exchange/docs/websocket-channels#status-channel)
- [Ticker Channel](https://docs.cloud.coinbase.com/exchange/docs/websocket-channels#ticker-channel)


this is controlled by the **subscription message** property in the Source configuration, e.g.

```bash
sourceConfig:
  - subscription : "{ "type": "subscribe", "channels": [
        { "name": "heartbeat", "product_ids": ["ETH-EUR"]}]}"
  - websocketURI: "wss://ws-feed.exchange.coinbase.com"
...
```

---
References

- https://docs.cloud.coinbase.com/exchange/docs/welcome#websocket-feed
- https://docs.cloud.coinbase.com/exchange/docs/websocket-overview