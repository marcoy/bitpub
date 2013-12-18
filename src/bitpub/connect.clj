(ns bitpub.connect
  (:require [environ.core :refer [env]]))


(defn get-conn-settings
  []
  "Get the RabbitMQ URL string from the environment variable. If none is found,
   throw an exception."
  (if (:rabbitmq-url env)
    {:uri (:rabbitmq-url env)}
    (throw (Exception. "Cannot get rabbitmq URL string. Try setting 'RABBITMQ_URL'."))))
