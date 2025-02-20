(ns metabase.api.query-description-test
  (:require
   [clojure.test :refer :all]
   [metabase.api.query-description :as api.qd]
   [metabase.models.metric :refer [Metric]]
   [metabase.models.segment :refer [Segment]]
   [metabase.models.table :refer [Table]]
   [metabase.test :as mt]
   [metabase.test.fixtures :as fixtures]
   [metabase.util.i18n :refer [deferred-tru]]
   [toucan.util.test :as tt]
   [toucan2.core :as t2]))

(use-fixtures :once (fixtures/initialize :db))

(deftest metrics-query-test
  (testing "queries"

    (testing "without any arguments, just the table"
      (is (= {:table "Venues"}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues))))))

    (testing "with limit"
      (is (= {:table "Venues"
              :limit 10}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:limit 10}))))))

    (testing "with cumulative sum of price"
      (is (= {:table       "Venues"
              :aggregation [{:type :cum-sum
                             :arg  "Price"}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:aggregation [[:cum-sum $price]]}))))))
    (testing "with equality filter"
      (is (= {:table  "Venues"
              :filter [{:field "Price"}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:filter [:= [$price 1234]]}))))))

    (testing "with order-by clause"
      (is (= {:table    "Venues"
              :order-by [{:field     "Price"
                          :direction :asc}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:order-by [[:asc $price]]}))))))

    (testing "with an aggregation metric"
      (tt/with-temp Metric [metric {:table_id   (mt/id :venues)
                                    :name       "Test Metric 1"
                                    :definition {:aggregation [[:count]]}}]
        (is (= {:table       "Venues"
                :aggregation [{:type :metric
                               :arg  "Test Metric 1"}]}
               (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                  (:query (mt/mbql-query venues
                                                                         {:aggregation [[:metric (:id metric)]]}))))))

      (is (= {:table       "Venues"
              :aggregation [{:type :metric
                             :arg  (deferred-tru "[Unknown Metric]")}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:aggregation [[:metric -1]]})))))

      ;; confirm that it doesn't crash for non-integer metrics
      (is (= {:table "Venues"}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:aggregation [[:metric "not-a-integer"]]}))))))

    (testing "with segment filters"
      (tt/with-temp Segment [segment {:name "Test Segment 1"}]
        (is (= {:table  "Venues"
                :filter [{:segment "Test Segment 1"}]}
               (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                  (:query (mt/mbql-query venues
                                                                         {:filter [[:segment (:id segment)]]}))))))

      (is (= {:table  "Venues"
              :filter [{:segment (deferred-tru "[Unknown Segment]")}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                (:query (mt/mbql-query venues
                                                                       {:filter [[:segment -1]]}))))))

    (testing "with named aggregation"
      (is (= {:table       "Venues"
              :aggregation [{:type :aggregation
                             :arg  "Nonsensical named metric"}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                {:aggregation [[:aggregation-options
                                                                [:sum [:*
                                                                       [:field (mt/id :venues :latitude) nil]
                                                                       [:field (mt/id :venues :longitude) nil]]]
                                                                {:display-name "Nonsensical named metric"}]]}))))

    (testing "with unnamed complex aggregation"
      (is (= {:table       "Venues"
              :aggregation [{:type :sum
                             :arg  ["Latitude" "*" "Longitude"]}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                {:aggregation [[:sum [:*
                                                                      [:field (mt/id :venues :latitude) nil]
                                                                      [:field (mt/id :venues :longitude) nil]]]]}))))

    (testing "with unnamed complex aggregation with multiple arguments"
      (is (= {:table       "Venues"
              :aggregation [{:type :sum
                             :arg  ["Latitude" "+" "Longitude" "+" "ID"]}]}
             (api.qd/generate-query-description (t2/select-one Table :id (mt/id :venues))
                                                {:aggregation [[:sum [:+
                                                                      [:field (mt/id :venues :latitude) nil]
                                                                      [:field (mt/id :venues :longitude) nil]
                                                                      [:field (mt/id :venues :id) nil]]]]}))))))
