;;prime_map это мапа в которой хранится число и его простые делители
(defn primes
  []
  (letfn [(sieve [prime_map n]
    (lazy-seq
      (if-let [prime_dividers (get prime_map n)]
        (sieve (-> prime_map
          (dissoc n) ;удаляется чтобы память не занимать
          ((fn [m]
            (reduce (fn [acc p]
                (let [next (+ n p)] ;следующее кратное вычисляется и засовывается в мапу
                  (update acc next (fnil conj []) p)))
              m
              prime_dividers))))
          (inc n))

          ;это иначе
        (cons n (sieve (assoc prime_map (* n n) [n]) (inc n))))))]
    (sieve {} 2)))

(println (take 10 (primes)))
