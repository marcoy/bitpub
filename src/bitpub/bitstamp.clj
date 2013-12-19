(ns bitpub.bitstamp
  (:require [clojure.core.async :as as :refer [alts! close! chan go go-loop <! >! put! take!]]
            [cheshire.core :as json]
            [org.httpkit.client :as hkit]
            [clj-http.client :as http]))


(def ^{:const true}
  bitstamp-ticker-url "https://www.bitstamp.net/api/ticker/")


(def ^{:const true}
  campbx-ticker-url "http://campbx.com/api/xticker.php")


(defn http-get
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
        feed (as/merge [campbx-feed bitstamp-feed])]
    (while true
      (println "Data:" (:body (as/<!! feed))))))
