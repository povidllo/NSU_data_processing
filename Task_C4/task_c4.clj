(ns task-c4)

(declare supply-msg)
(declare notify-msg)

(defn storage
      [ware notify-step & consumers]
      (let [counter (atom 0 :validator #(>= % 0)),
            worker-state {:storage counter,
                          :ware ware,
                          :notify-step notify-step,
                          :consumers consumers}]
           {:storage counter,
            :ware ware,
            :worker (agent worker-state)}))

(defn factory
      [amount duration target-storage & ware-amounts]
      (let [bill (apply hash-map ware-amounts),
            buffer (reduce-kv (fn [acc k _] (assoc acc k 0)) {} bill),
            worker-state {:amount amount,
                          :duration duration,
                          :target-storage target-storage,
                          :bill bill,
                          :buffer buffer}]
           {:worker (agent worker-state)}))

(defn source
  [amount duration target-storage]
  (new Thread
       (fn []
         (Thread/sleep duration)
         (send (target-storage :worker) supply-msg amount)
         (recur))))

(defn supply-msg
      [state amount]
      (swap! (state :storage) #(+ % amount))
      (let [ware (state :ware),
            cnt @(state :storage),
            notify-step (state :notify-step),
            consumers (state :consumers)]
           (when (and (> notify-step 0)
                      (> (int (/ cnt notify-step))
                         (int (/ (- cnt amount) notify-step))))
                 (println (.format (new java.text.SimpleDateFormat "hh.mm.ss.SSS") (new java.util.Date))
                          "|" ware "amount: " cnt))
           (when consumers
                 (doseq [consumer (shuffle consumers)]
                        (send (consumer :worker) notify-msg ware (state :storage) amount))))
      state)

(defn notify-msg
  [state ware storage-atom _]
  (let [requirements (:bill state)
        buf (:buffer state)
        required (requirements ware)]

    (if (nil? required)
      state

      (let [have (get buf ware 0)]
        (if (>= have required)
          state

          (let [missing (- required have)
                on-stock @storage-atom
                to-take (min missing on-stock)]

            (if (<= to-take 0)
              state

              (if (try
                    (swap! storage-atom #(- % to-take))
                    true
                    (catch IllegalStateException _ false))

                (let [updated-buf (assoc buf ware (+ have to-take))
                      complete? (every?
                                  (fn [[k v]] (>= (get updated-buf k 0) v))
                                  requirements)]

                  (if complete?
                    (do
                      (Thread/sleep (:duration state))
                      (send ((:target-storage state) :worker)
                            supply-msg
                            (:amount state))
                      (assoc state
                             :buffer (zipmap (keys requirements)
                                             (repeat 0))))

                    (assoc state :buffer updated-buf)))

                state))))))))



(def safe-storage (storage "Safe" 1))
(def safe-factory (factory 1 3000 safe-storage "Metal" 3))
(def cuckoo-clock-storage (storage "Clock" 1))
(def cuckoo-clock-factory (factory 1 2000 cuckoo-clock-storage "Lumber" 5 "Gears" 10))
(def gears-storage (storage "Gears" 20 cuckoo-clock-factory))
(def gears-factory (factory 4 1000 gears-storage "Ore" 4))
(def metal-storage (storage "Metal" 5 safe-factory))
(def metal-factory (factory 1 1000 metal-storage "Ore" 10))
(def lumber-storage (storage "Lumber" 20 cuckoo-clock-factory))
(def lumber-mill (source 5 4000 lumber-storage))
(def ore-storage (storage "Ore" 10 metal-factory gears-factory))
(def ore-mine (source 2 1000 ore-storage))

(def all-storages [safe-storage cuckoo-clock-storage gears-storage metal-storage lumber-storage ore-storage])

(def monitor-thread (atom nil))

(defn resource-monitor []
  (new Thread
    (fn []
      (try
        (loop []
          (Thread/sleep 2000)
          (doseq [s all-storages]
            (println (:ware s) ":" @(:storage s)))
          (println "Safe :" (:buffer @(:worker safe-factory)))
          (println "Clock :" (:buffer @(:worker cuckoo-clock-factory)))
          (recur))
        (catch InterruptedException _
          (println "Monitor stopped"))))))


(defn start []
      (.start ore-mine)
      (.start lumber-mill))
      (let [t (resource-monitor)]
           (.start t)
           (reset! monitor-thread t)))

(defn stop []
      (.interrupt ore-mine)
      (.interrupt lumber-mill))
      (when @monitor-thread
            (.interrupt @monitor-thread)
            (reset! monitor-thread nil)))


;; (defn start []
;;       (.start ore-mine)
;;       (.start lumber-mill))
;;
;; (defn stop []
;;       (.interrupt ore-mine)
;;       (.interrupt lumber-mill))

