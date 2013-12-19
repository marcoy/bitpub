(ns bitpub.bitstamp
  (:require [clojure.core.async :as as :refer [alts! close! chan go go-loop <! >! put!]]
            [cheshire.core :as json]
            [org.httpkit.client :as http]))


(def ^{:const true}
  ticker-url "https://www.bitstamp.net/api/ticker/")


(defn http-get
  [url]
  (let [c (chan)]
    (http/get url
              (fn [r] (put! c r)))
    c))


(defn poller
  "Poll the bitstamp's ticker URL to GET the data. In accordance to bitstamp's
   document, this only polls every 60 seconds. It places the value on the out
   channel."
  []
  (let [out (chan)]
    (go-loop []
      (let [time-out (as/timeout 60000)
            ticker-poll (http-get ticker-url)
            [v c] (alts! [time-out ticker-poll])]
        (cond
          (= c ticker-poll) (do (>! out v)
                                (close! c)
                                ;; park for a minute before polling again
                                (<! (as/timeout 60000))
                                (recur))
          (= c time-out) (recur))))
    out))


(defn -main
  [& args]
  (let [ch (poller)]
    (while true
      (println "Data:" (:body (as/<!! ch))))))
