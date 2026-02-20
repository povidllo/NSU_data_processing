(load-file "task.clj")

(require '[clojure.test :refer :all])

(deftest first-5-primes
  (is (= (take 5 (primes))
         '(2 3 5 7 11))))

(deftest first-10-primes
  (is (= (vec (take 10 (primes)))
         [2 3 5 7 11 13 17 19 23 29])))

(deftest hundredth-prime
  (is (= (nth (primes) 99) 541)))

(deftest odd-after-two
  (is (every? odd? (rest (take 50 (primes))))))

(deftest take-zero-lazy
  (is (= (take 0 (primes)) '())))

(run-tests)
