(ns statisk.core-test
  (:require [clojure.test :refer :all]
            [statisk.core :refer :all]))

(deftest sanity-test
  (testing "I succeed."
    (is (= 1 1))))
