(ns bitpub.examples.parproc
  (:require [clojure.core.async :as as :refer [alts! alts!! alt! timeout go thread
                                               close! put! go-loop chan >! <! >!!
                                               mult tap pub sub]]
            [org.httpkit.client :as http]
            [cheshire.core :as json]))

;; http://stuartsierra.com/2013/12/08/parallel-processing-with-core-async

(defn parallel
  "Processes values from input channel in parallel on n 'go' blocks.

   Invokes f on values taken from input channel. Values returned from f
   are written on output channel.

   Returns a channel which will be closed when the input channel is closed
   and all operations have completed.

   Notes: the order of outputs may not match the order of inputs."
  [n f input output]
  (let [tasks (doall
                (repeatedly n
                            #(go-loop []
                               (let [in (<! input)]
                                 (when-not (nil? in)
                                   (let [out (f in)]
                                     (when-not (nil? out)
                                       (>! output out))
                                     (recur)))))))]
    (go (doseq [task tasks]
          (<! task)))))


(defn pmax
  "Process messages from input in parallel with at most max concurrent
   operations.

   Invokes f on values taken from input channel. f must return a
   channel, whose first value (if not closed) will be put on the output
   channel.

   Returns a channel which will be closed when the input channel is
   closed and all operations have completed.

   Creates new operations lazily: if processing can keep up with input,
   the number of parallel operations may be less than max.

   Note: the order of outputs may not match the order of inputs."
  [max f input output]
  (go-loop [tasks #{input}]
    (when (seq tasks)
      (let [[value task] (alts! (vec tasks))]
        (if (= task input)
          ;; We get more incoming tasks. If there are more tasks than this is
          ;; configured to handle (based on the value of max), it will
          ;; temporarily not listen on the input channel. When a task is
          ;; finsihed (else branch), the input channel will be put back to the
          ;; set of channels to listen to for alts!.
          (if (nil? value)
            (recur (disj tasks task))  ; input is closed
            (recur (conj (if (= max (count tasks))  ; max - 1 task running
                           (disj tasks input)  ; temporarily stop reading input
                           tasks)
                         (f value))))
          ;; one processing task finished: continue reading input
          ;; tasks is a set, so it can always (conj input) to tasks.
          (do (when-not (nil? value) (>! output value))
              (recur (-> tasks (disj task) (conj input)))))))))


(defn sink
  "Returns an atom containing a vector. Consumes values from channel
   ch and conj's them into the atom."
  [ch]
  (let [a (atom [])]
    (go-loop []
      (let [v (<! ch)]
        (when-not (nil? v)
          (swap! a conj v)
          (recur))))
    a))


(defn watch-counter
  [counter thread-counts]
  (add-watch counter
             :thread-counts
             (fn [_ _ _ thread-count]
               (swap! thread-counts conj thread-count))))

;; https://github.com/halgari/clojure-conj-2013-core.async-examples

(defn thread-pool-service
  [ch f max-threads timeout-ms]
  (let [thread-count (atom 0)
        buffer-status (atom 0)
        buffer-chan (chan)
        thread-fn (fn []
                    (swap! thread-count inc)
                    (loop []
                      (when-let [v (first (alts!! [buffer-chan (timeout timeout-ms)]))]
                        (f v)
                        (recur)))
                    (swap! thread-count dec)
                    (println "Exiting ..."))]
    (go (loop []
          (when-let [v (<! ch)]
            (if-not (alt! [[buffer-chan v]] true
                          :default false)
              (loop []
                (if (< @thread-count max-threads)
                  (do (put! buffer-chan v)
                      (thread (thread-fn)))
                  (when-not (alt! [[buffer-chan v]] true
                                  [(timeout 1000)] ([_] false))
                    (recur)))))
            (recur)))
        (close! buffer-chan))))


(def exec-chan (chan))
(def thread-pool (thread-pool-service exec-chan
                                      (fn [x]
                                        (println x)
                                        (Thread/sleep 5000)) 3 3000))

;; (>!! exec-chan "Hello World")


;;
;; HTTP Async
;;
(defn http-get
  [url]
  (let [c (chan)]
    (println url)
    (http/get url
              (fn [r] (put! c r)))
    c))


(def apikey "b4cb6cd7a349b47ccfbb80e05a601a7c")


(defn request-and-process
  [url]
  (go
    (-> (str "http://api.themoviedb.org/3/" url "api_key=" apikey)
        http-get
        <!
        :body
        (json/parse-string true))))


(defn latest-movies
  []
  (request-and-process "movies/latest?"))


(defn top-rated-movies
  []
  (request-and-process "movie/top_rated?"))


;; (<!! (top-rated-movies))


(defn movies-by-id
  [id]
  (request-and-process (str "movie/" id "/casts?")))


(defn movie-cast
  [id]
  (request-and-process (str "movie/" id "/casts?")))


;;
;; Mult
;;
;; Create a mult. This allows data from one channel to be broadcast to many
;; other channels that "tap" the mult.
(def to-mult (chan 1))
(def m (mult to-mult))


(let [c (chan 1)]
  (tap m c)
  (go (loop []
        (when-let [v (<! c)]
          (println "Got!" v)
          (recur))
        (println "Exiting!"))))

(>!! to-mult 42)
(>!! to-mult 43)
(close! to-mult)


;;
;; Pub/Sub
;;
;; This a bit like Mult + Multimethods
(def to-pub (chan 1))
(def p (pub to-pub :tag))

(def print-chan (chan 1))


(go (loop []
      (when-let [v (<! print-chan)]
        (println v)
        (recur))))


;; This guy likes updates about cats.
(let [c (chan 1)]
  (sub p :cats c)
  (go (println "I like cats:")
      (loop []
        (when-let [v (<! c)]
          (>! print-chan (pr-str "Cat guy got: " v))
          (recur))
        (println "Cat guy exiting"))))


;; This guy likes updates about dogs.
(let [c (chan 1)]
  (sub p :dogs c)
  (go (println "I like dogs:")
      (loop []
        (when-let [v (<! c)]
          (>! print-chan (pr-str "Dog guy got: " v))
          (recur))
        (println "Dog guy exiting"))))

;; This guy likes updates about animals.
(let [c (chan 1)]
  (sub p :dogs c)
  (sub p :cats c)
  (go (println "I like cats or dogs:")
      (loop []
        (when-let [v (<! c)]
          (>! print-chan (pr-str "Cat/Dog guy got: " v))
          (recur))
        (println "Cat/Dog guy exiting"))))


(defn send-with-tags
  [msg]
  (doseq [tag (:tags msg)]
    (println "sending..." tag)
    (>!! to-pub {:tag tag
                 :msg (:msg msg)})))


(defn pubsub-test
  []
  (send-with-tags {:msg "New Cat Story"
                   :tags [:cats]})

  (send-with-tags {:msg "New Dog Storg"
                   :tags [:dogs]})

  (send-with-tags {:msg "New Pet Story"
                   :tags [:cats :dogs]})
  (close! to-pub))
