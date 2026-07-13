(ns fishery.store
  "SSoT for the ISCO-08 6222 independent small-scale fishery
  operations actor (itonami actor pattern, ADR-2607011000 /
  CLAUDE.md Actors section; README's 'Robotics premise' — a
  net-monitoring and catch-sorting robot performs gear checks and
  catch documentation under this advisor/governor pair, which never
  dispatches hardware itself). Modeled on cloud-itonami-isco-4311's
  bookkeeping.store.

  Domain:

    client  — a registered organization (:client-id, :name)
    fishery — a registered fishery allocation {:fishery-id :client-id
              :name :quota-kg-remaining number
              :protected-species-zones #{zone-str}}.
              `:quota-kg-remaining` is the registered legal allocation
              a proposed catch must not exceed (quota is a legal
              allocation, not a target to approach);
              `:protected-species-zones` is the registered set a
              proposed catch's zone must NOT be a member of (no gear
              deployment near protected-species zones without the
              governor gate).
    record  — a committed operating record (approved catch) —
              written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (fishery [s fishery-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-fishery! [s f])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (fishery [_ fishery-id] (get-in @a [:fisheries fishery-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-fishery! [s f]
    (swap! a assoc-in [:fisheries (:fishery-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :fisheries {} :records [] :ledger []}
                                   seed)))))
