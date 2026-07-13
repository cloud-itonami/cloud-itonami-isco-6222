(ns fishery.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [fishery.actor :as actor]
            [fishery.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Fishery"})
    (store/register-fishery! st {:fishery-id "F-1" :client-id "client-1"
                                 :name "bay-lot-4"
                                 :quota-kg-remaining 200
                                 :protected-species-zones #{"reef-north"}})
    st))

(deftest commits-an-in-quota-non-protected-catch
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-catch :stake :low
                 :fishery-id "F-1" :catch-kg 150 :zone "open-bay"}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-a-protected-zone-catch
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-catch :stake :low
                 :fishery-id "F-1" :catch-kg 150 :zone "reef-north"}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-reports-bycatch-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :report-bycatch-incident :stake :low
                 :fishery-id "F-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
