# Bitpub

A Clojure application that publishes Bitcoin market data to a RabbitMQ borker.
You will need to define an environment variable named `RABBITMQ_URL` which
should contain the value of the RabbitMQ connection string.

The supported exchanges are:
CampBx - https://campbx.com/
Bitstamp - https://www.bitstamp.net/
Vircurex - https://vircurex.com/
BTC-e - https://btc-e.com/
BTC China - https://vip.btcchina.com/

## Usage

To print out the feeds data to `STDOUT`
```
lein run -m bitpub.ticker-feed
```

To publish the market data to a RabbitMQ broker.
```
lein run -m bitpub.publish
```

There is a consumer that will simply print all the feed data.
```
lein run -m bitpub.consumer
```

## License

Copyright © 2014 Marco Yuen <marcoy@gmail.com>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
