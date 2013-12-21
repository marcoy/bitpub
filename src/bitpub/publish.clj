(ns bitpub.publish
  (require [clojure.core.async :as as :refer [go-loop <!]]
           [cheshire.core      :as json]
           [langohr.core       :as rmq]
           [langohr.channel    :as lch]
           [langohr.exchange   :as le]
           [langohr.queue      :as lq]
           [langohr.consumers  :as lc]
           [langohr.basic      :as lb]
           [bitpub.ticker-feed :as feed]
           [bitpub.connect :refer [get-conn-settings]]))


(def ^{:const true}
  ename "bitpub.ticker")


(defn publisher
  [mq-conn feed-name feed-create-fn]
  (let [feed (feed-create-fn)
        topic (format "ticker.%s" (name feed-name))
        mq-ch (lch/open mq-conn)]
    (go-loop []
             (let [data (<! feed)]
               (try
                 (lb/publish mq-ch ename topic (json/generate-string data))
                 (catch Exception e
                   (println  (.getMessage e))))
               (recur)))))


(defn start-publishing
  []
  (let [conn (rmq/connect (get-conn-settings))
        ch (lch/open conn)
        supported-feeds (seq feed/supported-feeds)]
    (do
      (le/declare ch ename "topic" :durable true :auto-delete false)
      (doseq [[feed-name feed-fn] supported-feeds]
        (publisher conn feed-name feed-fn))
      (println "Press any key to quit")
      (read-line)
      (rmq/close ch)
      (rmq/close conn))))


(defn -main
  []
  (start-publishing))
