(ns bitpub.consumer
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


(defn -main
  [& args]
  (let [conn (rmq/connect (get-conn-settings))
        ch (lch/open conn)
        q (:queue (lq/declare ch "all-feeds" :exclusive false :auto-delete false))
        handler (fn [ch {:keys [routing-key] :as metadata} ^bytes payload]
                  (let [jmsg (json/parse-string (String. payload "UTF-8") true)]
                    (println (format "%s (rk: %s)" (:source jmsg) routing-key))))]
    (lq/bind ch q ename :routing-key "ticker.#")
    (lc/subscribe ch q handler :auto-ack true)
    (println "Subscribed")))
