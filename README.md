# cloud-itonami-isco-6222

Open Occupation Blueprint for **ISCO-08 6222**: Inland and Coastal Waters Fishery Workers.

**Maturity: `:implemented`** — FisheryAdvisor ⊣ FisheryGovernor as a
langgraph StateGraph (`intake → advise → govern → decide →
commit/hold`, human-approval interrupt), modeled on
cloud-itonami-isco-4311's bookkeeping actor. 14 tests / 29 assertions
green. The governor never dispatches hardware — it only gates what
the net-monitoring/catch-sorting robot below may execute.

The catch HARD invariants — arithmetic and set exclusion, not
discretion:

1. **Quota ceiling** — a proposed catch must not exceed the
   registered remaining quota (quota is a legal allocation, not a
   target to approach).
2. **Protected-zone exclusion** — a proposed catch's zone must NOT be
   a member of the registered protected-species-zones set.

`:approve-vessel-operation` and `:report-bycatch-incident` **always**
escalate to human sign-off regardless of confidence, per this repo's
Trust Controls (business-model.md) — bycatch incidents can never be
suppressed.

This repository designs a forkable OSS business for an independent small-scale inland/coastal fishery worker: a net-monitoring and catch-sorting robot performs gear checks and catch documentation under a governor-gated actor, so the operator keeps their own catch and quota records instead of renting a closed fishery-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a net-monitoring and catch-sorting robot performs gear checks and catch documentation under an actor that proposes
actions and an independent **Fishery Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
operating near vessels, deep water, or protected-species bycatch) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
fishing plan + quota allocation + catch-documentation requirement
        |
        v
Fishery Advisor -> Fishery Governor -> harvest/sort, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6222`). Required capabilities:

- :robotics
- :telemetry
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
