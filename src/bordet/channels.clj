(ns bordet.channels
  (:require [clojure.core.async :as a]
            [bordet.control :as control]
            [bordet.config :as config]
            [bordet.redis :as redis]
            [bordet.json :as json]
            [taoensso.timbre :refer [info debug]]))

(defmulti xform (fn [ns-] (keyword ns- "dashboard")))

(def tick (a/chan 1 (map (fn [i]
                         (debug "Ticking with value" i)
                         i))))
(def trigger (a/pub tick :event))

(def out (a/chan 10 (map (fn [i]
                           (info "Got msg in sink" i)
                           i))))
(def sink (a/mix out))

(def tasks (atom []))

(defn force-tick [id]
  (info "Forcing tick with id" id)
  (a/go (a/>! tick {:event :tick
                    :value id})))

(defn task-xf [ns-]
  (comp
    (map :value)
    (filter (some-fn #{::refresh (keyword ns- "refresh")}
                     (fn [ping-nr]
                       (and (number? ping-nr)
                            (zero? (mod ping-nr
                                        (config/get-config (keyword ns- "refresh-rate"))))))))
    (xform ns-)))

(defn subscribe-and-connect! [ns-]
  (info "Starting client for" ns-)
  (let [ch (a/chan 1 (comp
                       (map (fn [i]
                              (info ns- "called with value" i)
                              i))
                       (task-xf ns-)
                       (map (fn [i]
                              (info "Invoke went through.")
                              i))
                       (map (partial vector (keyword ns- "dashboard")))))
        _ (a/sub trigger :tick ch)
        _ (a/admix sink ch)]
    ch))

(defn start-ticker! []
  ;; ticks every minute, 1-120
  (info "Starting Ticker")
  (a/go-loop [ix 1]
             (a/<! (a/timeout 60000))
             (a/>! tick {:event :tick :value ix})
             (recur (inc (mod ix 120)))))

(defn stop-ticker! []
  (info "Stopping Ticker")
  (a/close! tick))

(defn start-sink! []
  (info "Startink Sink")
  (a/go-loop [[key- data] (a/<! out)]
             (if (some? data)
               (let [rk (str (namespace key-) "/" (name key-))
                     rv (json/data->json data)]
                 (redis/set-value rk rv))
               (info "Task" key- "published empty data" data))
             (recur (a/<! out))))

(defn stop-sink! []
  (info "Stopping Sink")
  (a/close! out))

(defn start-task! [ns-]
  (swap! tasks conj (subscribe-and-connect! ns-)))

(defn start-tasks! [coll]
  (run! start-task! coll))

(defn stop-tasks! []
  (info "Stopping all tasks")
  (run! a/close! @tasks)
  (reset! tasks []))

(defmethod control/message-handler ::force [[_ _ _ refresh]]
  (info "Forcing refresh")
  (force-tick (cond
    (#{"all" ""} refresh) ::refresh
    :else (keyword refresh "refresh"))))
