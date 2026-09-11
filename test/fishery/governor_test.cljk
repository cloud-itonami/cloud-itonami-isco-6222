(ns fishery.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [fishery.store :as store]
            [fishery.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Fishery"})
    (store/register-fishery! st {:fishery-id "F-1" :client-id "client-1"
                                 :name "bay-lot-4"
                                 :quota-kg-remaining 200
                                 :protected-species-zones #{"reef-north"}})
    st))

(defn- catch-op [kg zone]
  {:op :approve-catch :effect :propose :fishery-id "F-1"
   :catch-kg kg :zone zone :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-quota-and-non-protected-zone
  (let [st (fresh-store)
        v (governor/check req {} (catch-op 150 "open-bay") st)]
    (is (:ok? v))))

(deftest ok-at-exact-quota
  (testing "catch exactly at the remaining quota is within margin"
    (let [st (fresh-store)
          v (governor/check req {} (catch-op 200 "open-bay") st)]
      (is (:ok? v)))))

(deftest hard-on-quota-exceeded
  (testing "quota is a legal allocation, not a target to approach"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (catch-op 400 "open-bay") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :quota-exceeded (:rule %)) (:violations v))))))

(deftest hard-on-protected-zone-violation
  (testing "no gear deployment near protected-species zones without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (catch-op 150 "reef-north") :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :protected-zone-violation (:rule %)) (:violations v))))))

(deftest hard-on-unknown-fishery
  (let [st (fresh-store)
        v (governor/check req {} (assoc (catch-op 150 "open-bay") :fishery-id "F-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-fishery (:rule %)) (:violations v)))))

(deftest hard-on-foreign-fishery
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (catch-op 150 "open-bay") st)]
      (is (:hard? v))
      (is (some #(= :fishery-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (catch-op 150 "open-bay") st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (catch-op 150 "open-bay") :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-vessel-operation-even-at-high-confidence
  (testing "operating near vessels/deep water requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-vessel-operation :effect :propose
                                    :fishery-id "F-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-bycatch-report-even-at-high-confidence
  (testing "bycatch incidents cannot be suppressed"
    (let [st (fresh-store)
          v (governor/check req {} {:op :report-bycatch-incident :effect :propose
                                    :fishery-id "F-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (catch-op 150 "open-bay") :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
