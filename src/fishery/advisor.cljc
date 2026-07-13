(ns fishery.advisor
  "FisheryAdvisor — the advisor named in this repository's README,
  proposing a fishery operation (approve a catch, approve a vessel
  operation, report a bycatch incident) from a fishing plan, quota
  allocation and catch-documentation requirement. Swappable mock/llm;
  the advisor ONLY proposes — `fishery.governor` checks the quota
  ceiling and protected-zone exclusion independently and always
  escalates vessel-operation/bycatch decisions. Modeled on
  cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-catch|:approve-vessel-operation|:report-bycatch-incident
               :effect :propose :fishery-id str :catch-kg number
               :zone str :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake fishery-id catch-kg zone] :as request}]
  {:op op
   :effect :propose
   :fishery-id fishery-id
   :catch-kg catch-kg
   :zone zone
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a fishery-operations advisor. Given a request, propose an
   :op, the :fishery-id, :catch-kg and :zone, an honest :confidence
   and a :stake. Never call an over-quota catch or a protected-zone
   deployment conforming — the governor checks both against the
   registered fishery record. Vessel-operation and bycatch decisions
   always require human sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
