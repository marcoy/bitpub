(ns bitpub.ticker-feed-test
  (:require [clojure.test :refer [deftest testing is are]]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [clojure.core.async :refer [<!!] :as as]
            [cheshire.core :as json]
            [clj-http.fake :refer [with-fake-routes with-fake-routes-in-isolation]]
            [bitpub.ticker-feed :refer :all]))


(deftest bitstamp-tranform-fn
  (testing "Bitstamp transform function using live data"
    (let [bitstamp-feed (create-bitstamp-feed)
          data (<!! bitstamp-feed)]
      (is (map? data))
      (are [x y] (= x y)
           (:source data) :bitstamp
           (sort (keys data)) '(:ask :bid :high :last :low :source :timestamp :volume)))))


(deftest bitstamp-tranform-fn
  (testing "CampBX transform function using live data"
    (let [campbx-feed (create-campbx-feed)
          data (<!! campbx-feed)]
      (is (map? data))
      (are [x y] (= x y)
           (:source data) :campbx
           (sort (keys data)) '(:best_ask :best_bid :last_trade :source)))))


(deftest vircurex-tranform-fn
  (testing "Vircurex transform function using live data"
    (let [vircurex-feed (create-vircurex-feed)
          data (<!! vircurex-feed)]
      (is (map? data))
      (are [x y] (= x y)
           (:source data) :vircurex
           (sort (keys data)) '(:alt :base :highest_bid :last_trade :lowest_ask :source :volume)))))


(deftest btce-tranform-fn
  (testing "BTC-e transform function using live data"
    (let [btce-feed (create-btce-feed)
          data (<!! btce-feed)]
      (is (map? data))
      (are [x y] (= x y)
           (:source data) :btce
           (sort (keys data)) '(:source :ticker)))))


(deftest btcchina-tranform-fn
  (testing "BTCChina transform function using live data"
    (let [btcchina-feed (create-btcchina-feed)
          data (<!! btcchina-feed)]
      (is (map? data))
      (are [x y] (= x y)
           (:source data) :btcchina
           (sort (keys data)) '(:source :ticker)))))
