(ns dining-philosophers
  (:require [clojure.pprint :refer [pprint]])
  (:import (java.util.concurrent Executors TimeUnit)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CONFIG
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def philosophers-count 6)         ;; число философов
(def think-ms 100)                 ;; время "думания" (мс)
(def eat-ms 100)                   ;; время "еды" (мс)
(def cycles-per-philosopher 10)    ;; сколько циклов (думать->есть)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STM VARIABLES
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def forks
  "Массив из ref-ов, каждый — вилка со счётчиком использования"
  (vec (repeatedly philosophers-count #(ref {:uses 0}))))

(def transaction-restarts
  "Счётчик перезапусков транзакций"
  (atom 0))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STM WRAPPERS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro dosync-count-retries
  "Обёртка над dosync, увеличивающая атом перезапусков при retry"
  [& body]
  `(try
     (dosync ~@body)
     (catch clojure.lang.PersistentQueue$Node e#
       (swap! transaction-restarts inc)
       (throw e#))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PHILOSOPHER LOGIC
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn philosopher
  "Один философ:
   - думает
   - пытается взять вилки
   - ест
   - возвращает вилки
   Повторяет cycles-per-philosopher раз"
  [id]
  (let [left-fork  (forks id)
        right-fork (forks (mod (inc id) philosophers-count))]
    (dotimes [i cycles-per-philosopher]
      ;; думаем
      (Thread/sleep think-ms)

      ;; берём вилки
      (dosync-count-retries
        (alter left-fork  update :uses inc)
        (alter right-fork update :uses inc))

      ;; едим
      (Thread/sleep eat-ms))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; EXECUTION
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn run-simulation []
  (let [executor (Executors/newFixedThreadPool philosophers-count)
        start    (System/currentTimeMillis)]

    ;; Запускаем философов
    (doseq [id (range philosophers-count)]
      (.submit executor #(philosopher id)))

    ;; Ждём завершения
    (.shutdown executor)
    (.awaitTermination executor 5 TimeUnit/MINUTES)

    (let [total-time (- (System/currentTimeMillis) start)]
      {:total-time-ms total-time
       :retries       @transaction-restarts
       :fork-uses     (mapv deref forks)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ENTRY POINT
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main []
  (pprint (run-simulation)))

(main)
