(ns bitpub.ticker-feed
  (:require [clojure.core.async :as as :refer [alts! close! chan go go-loop <! >! put! take!]]
            [cheshire.core :as json]
            [org.httpkit.client :as hkit]
            [clj-http.client :as http]))


(def ^{:const true}
  bitstamp-ticker-url "https://www.bitstamp.net/api/ticker/")


(def ^{:const true}
  campbx-ticker-url "http://campbx.com/api/xticker.php")


;; https://vircurex.com/welcome/api?locale=en
(def ^{:const true}
  vircurex-ticker-url "https://vircurex.com/api/get_info_for_1_currency.json?base=BTC&alt=USD")


;; https://btc-e.com/page/2
(def ^{:const true}
  btce-ticker-url "https://btc-e.com/api/2/btc_usd/ticker")


(def ^{:const true}
  btcchina-ticker-url "https://data.btcchina.com/data/ticker")


(defn http-get
  "Wrap a HTTP GET request in a go block. This returns a channel."
  [url]
  (go
    (http/get url)))


(defn create-ticker-feed
  "Poll the given ticker URL to GET the market data. It tries to place the value
   to the out channel. If no process is consuming the channel, it will wait for a
   specified time and poll the ticker URL again. This ensures the data that a
   consumer gets are kept somewhat up-to-date. There is a timeout for the initial
   HTTP GET. After the HTTP GET timeout has elapsed, it will retry again.

   Options:
   :get-timeout The time it waits for a reply from the GET request before
                retrying(in ms).
   :async-put-timeout The time it waits for a consumer to consume the value
                      (in ms).
   :park-time-fn A function that returns an integer. The integer will be used
                  as the park time before it re-polls the ticker url again."
  [ticker-url & {:keys [get-timeout async-put-timeout park-time-fn] :as params}]
  (let [out (chan)]
    (go-loop []
      (let [time-out (as/timeout get-timeout)
            ticker-poll (http-get ticker-url)
            [v c] (alts! [time-out ticker-poll])]
        (cond
          (= c ticker-poll) (let [[_ ch] (alts! [[out v] (as/timeout async-put-timeout)])]
                              ;; If no-one is consuming the out channel, polls
                              ;; the url again after 10s
                              (if (= ch out)
                                (do
                                  ;; park for a little bit before polling again
                                  (<! (as/timeout (park-time-fn)))
                                  (recur))
                                (recur)))
          ;; HTTP GET timeout
          (= c time-out) (do (close! c)
                             (recur)))))
    out))


(defn -main
  [& args]
  (let [campbx-feed (create-ticker-feed campbx-ticker-url
                                        :get-timeout 30000
                                        :async-put-timeout 10000
                                        :park-time-fn #(+ 1000 (rand-int 500)))
        bitstamp-feed (create-ticker-feed bitstamp-ticker-url
                                          :get-timeout 30000
                                          :async-put-timeout 10000
                                          :park-time-fn #(+ 1000 (rand-int 1500)))
        vircurex-feed (create-ticker-feed vircurex-ticker-url
                                          :get-timeout 30000
                                          :async-put-timeout 10000
                                          :park-time-fn #(+ 5000 (rand-int 500)))
        btce-feed (create-ticker-feed btce-ticker-url
                                      :get-timeout 30000
                                      :async-put-timeout 10000
                                      :park-time-fn #(+ 1000 (rand-int 1000)))
        btcchina-feed (create-ticker-feed btcchina-ticker-url
                                          :get-timeout 30000
                                          :async-put-timeout 10000
                                          :park-time-fn #(+ 2000 (rand-int 1000)))
        feed (as/merge [vircurex-feed campbx-feed bitstamp-feed btce-feed])]
    (while true
      (println "Data:" (:body (as/<!! feed))))))
