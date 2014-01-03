(defproject bitpub "0.1.0-SNAPSHOT"
  :description "FIXME: write description"

  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repl-options {:init-ns bitpub.publish
                 :init (do
                         (use 'bitpub.connect)
                         (require '[clojure.core.async :as as]
                                  '[org.httpkit.client :as hkit]
                                  '[clj-http.client    :as http]
                                  '[cheshire.core      :as json]
                                  '[langohr.core       :as rmq]
                                  '[langohr.channel    :as lch]
                                  '[langohr.exchange   :as le]
                                  '[langohr.queue      :as lq]
                                  '[langohr.consumers  :as lc]
                                  '[langohr.basic      :as lb]
                                  '[bitpub.ticker-feed :as feed]))}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [cheshire "5.3.0"]
                 [http-kit "2.1.15"]
                 [clj-http "0.7.8"]
                 [clj-http-fake "0.4.1"]
                 [environ "0.4.0"]
                 [com.novemberain/langohr "2.0.1"]]

  :plugins [[lein-environ "0.4.0"]])
