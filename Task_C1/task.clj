(defn task [alphabet n]
  (if (= n 1)
    alphabet
    (reduce 
      (fn [acc _]
        (mapcat
          (fn [s] 
            (map
              (fn [ch] (str s ch))
              (remove #(= (subs s (dec (count s))) %) alphabet)
              )
            )
          acc))
      alphabet
      (range 1 n))))

(prn (task ["a" "b" "c" "d"] 3))

