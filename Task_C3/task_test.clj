(load-file "task.clj")

(require '[clojure.test :refer :all])

(deftest test-empty
    (is (= '() (pfilter identity [])))
    (is (= '() (pfilter identity nil))))

(deftest all
    (is (= (range 10) (pfilter (constantly true) '(0 1 2 3 4 5 6 7 8 9)))))

(deftest even
    (is (= '(0 2 4 6 8)) (pfilter even? (range 10)))
)

(deftest odd
    (is (= '(1 3 5 7 9)) (pfilter odd? (range 10)))
)

(run-tests)
