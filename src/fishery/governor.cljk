(ns fishery.governor
  "FisheryGovernor — the independent safety/traceability layer named
  in this repository's README/business-model.md, gating the
  robot-dispensed physical work (gear checks, catch documentation) an
  advisor may propose. The governor never dispatches hardware itself.
  Modeled on cloud-itonami-isco-4311's bookkeeping.governor. Catch
  twist: a proposed catch is arithmetic comparison against the
  registered remaining quota — quota is a legal allocation, not a
  target to approach — and a proposed catch zone is either a member
  of the registered protected-species-zones set or it is not; gear
  deployment there requires the governor gate.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. fishery basis        — a catch approval must cite a REGISTERED
                           fishery allocation belonging to this
                           client.
    4. quota ceiling        — the proposed catch-kg must not exceed
                           the fishery's registered
                           :quota-kg-remaining (quota is a legal
                           allocation, not a target to approach).
    5. protected-zone exclusion — the proposed zone must NOT be a
                           member of the fishery's registered
                           :protected-species-zones set (no gear
                           deployment there without the governor
                           gate).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-vessel-operation (operating near vessels/deep
                           water requires human sign-off).
    7. :op :report-bycatch-incident (bycatch incidents cannot be
                           suppressed — always surfaced to a human,
                           never silently auto-committed).
    8. low confidence (< `confidence-floor`)."
  (:require [fishery.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-vessel-operation
                                     :report-bycatch-incident})

(defn- hard-violations [{:keys [request proposal]} client-record f]
  (let [{:keys [op catch-kg zone]} proposal
        catch? (= :approve-catch op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and catch? (nil? f))
      (conj {:rule :unknown-fishery :detail "未登録 fishery への漁獲承認は不可"})

      (and catch? f (not= (:client-id f) (:client-id request)))
      (conj {:rule :fishery-wrong-client :detail "fishery が別 client のもの"})

      (and catch? f (number? catch-kg) (> catch-kg (:quota-kg-remaining f)))
      (conj {:rule :quota-exceeded
             :detail (str "漁獲量 " catch-kg "kg > 残余クォータ " (:quota-kg-remaining f)
                          "kg（クォータは法的割当であって目標接近値ではない）")})

      (and catch? f zone (contains? (:protected-species-zones f) zone))
      (conj {:rule :protected-zone-violation
             :detail (str "海域 " zone " は登録済み保護種区域集合の要素（governor ゲート無しの漁具展開は許可されない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `fishery.store/Store`. Pure — never mutates the
  store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        f (some->> (:fishery-id proposal) (store/fishery store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record f)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
