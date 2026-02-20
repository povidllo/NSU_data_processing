(defn pfilter
  [func arr]
  (let [n (count arr)]
    (if (zero? n)
      '()
      (let [block-size 10
            num-threads 12]
        (letfn [(processOneBlock [blocks current_futures]
                  (lazy-seq
                    (cond

                      (and (seq blocks)
                           (< (count current_futures) num-threads))
                      (let [next-block (first blocks)
                            new-fut (future (doall (filter func next-block)))]
                        (processOneBlock (rest blocks) (conj current_futures new-fut)))

                      (seq current_futures)
                      (let [res (first current_futures)]
                        (concat @res (processOneBlock blocks (rest current_futures))))

                      :else nil)))]
          (processOneBlock (partition-all block-size arr) []))))))

(time (doall (filter (fn[x] (Thread/sleep 50) (even? x)) (range 40))))

(time (doall (pfilter (fn[x] (Thread/sleep 50) (even? x)) (range 40))))
