# Verified engine behavior (ModSecurity3 vs Coraza)

Facts established by `EngineBehaviorProbeTest` (run manually:
`DOCKER_JAVA_PROPERTIES="api.version=1.44" mvn test -Dtest=EngineBehaviorProbeTest -DrunEngineProbes=true`).
Generator design decisions reference this file. Last run: 2026-07-04 against
`ghcr.io/cognitivegears/coraza-validate-server:latest` and `owasp/modsecurity-crs:nginx`.

## JSON body flattening

- Both engines flatten JSON bodies into `ARGS` with a `json.` prefix.
- Array elements: ModSecurity3 keys are `json.items.array_0`, Coraza `json.items.0`
  (generated selectors use `(?:array_)?\d{1,9}` to match both).
- Coraza also lists container nodes (`json.category`, `json.photoUrls`) in
  `ARGS_NAMES`, not just leaves — allowlists must include intermediate prefixes.
- **JSON `null` flattens to a present key with an EMPTY value on both engines**
  (indistinguishable from `""`). Nullable properties are therefore validated with
  an optional-wrapped value pattern; a required-presence rule still sees the key.

## Request bodies

- `REQBODY_ERROR` does **not** fire for an empty body with `Content-Type:
  application/json` on either engine (the JSON processor accepts empty input).
- A request with no `Content-Type` header is reliably detectable with
  `&REQUEST_HEADERS:Content-Type "@eq 0"` on both engines — this is the
  optional-requestBody gate.
- ModSecurity3 sets `REQBODY_ERROR` for malformed JSON; Coraza does not (its
  `@validateSchema` rejects malformed JSON instead).

## multipart/form-data and form-urlencoded

- Text parts of multipart bodies land in `ARGS_POST` on **both** engines, so the
  same param rules and `ARGS_NAMES` allowlist cover urlencoded and multipart.
- File parts appear in `FILES_NAMES` (both engines), not in `ARGS_POST`.
- `REQBODY_ERROR` did not fire for well-formed multipart on either engine.

## Per-field enforcement limits derived from flattening

- ModSecurity3 lists only **leaf** keys in ARGS (no container nodes), so
  object-property counts (`minProperties`/`maxProperties`) and object-array
  element counts cannot be counted reliably per-field: an object with only
  nested-object members produces no countable key of its own. These constraints
  are enforced via schema.json on Coraza only.
- Array element counts (`minItems`/`maxItems`) ARE enforced per-field for
  arrays of primitives (their indexed leaf keys are countable on both engines).

## Coraza `@validateSchema` (JSON Schema)

Coraza's validator honors modern keywords regardless of the declared `$schema`
draft. Verified enforced under a draft-07 `$schema`: `const`,
`dependentRequired`, `if`/`then`/`else`, `prefixItems`, `patternProperties`,
`propertyNames`. Verified **ignored**: legacy draft-07 `dependencies` — always
emit `dependentRequired`, never `dependencies`. schema.json therefore stays
draft-07 with modern keywords copied verbatim from the raw spec.

Note: openapi-generator's normalizer rewrites the parsed spec in place (it
drops 3.1 `prefixItems`, among others), so raw keyword lookups re-parse the
original document (`Modsecurity3Generator.rawOpenAPI()`).

Per-field (SecRule) coverage of the long tail: `const` → exact-match value
rule; `dependentRequired` (body root) → chained presence rules;
`patternProperties` → name-scoped allowlist entries + typed value rules.
`prefixItems`, `contains`, `if`/`then`/`else`, `propertyNames`,
`unevaluatedProperties` are schema.json-only (Coraza).

ModSecurity3 has no JSON `@validateSchema` (XSD only), which is why the
modsecurity3 flavor relies on per-field rules.

## XML

- **Coraza fails config load** on `ctl:requestBodyProcessor=XML` /
  `SecRule XML "@validateSchema ..."` — the coraza flavor cannot get XML
  validation; it relies on content-type gating only.
- ModSecurity3 accepts `SecRule XML "@validateSchema <xsd>"` at load, sets
  `REQBODY_ERROR` on malformed XML, **but the operator matched valid and
  XSD-violating documents alike in the probe** — suspected runtime XSD load/parse
  failure matching everything. Needs investigation before the XSD subsystem
  ships (see Phase 6); do not assume `@validateSchema` works until a
  valid-document probe returns pass-through.

## Container test harness

- The Testcontainers wait strategy expects `GET /` to answer 200 or 403; probe
  rule sets must include a health-check bypass
  (`SecRule REQUEST_FILENAME "@streq /" "phase:1,pass,nolog,ctl:ruleEngine=Off"`).
- `owasp/modsecurity-crs:nginx` publishes linux/386 only — on other
  architectures `docker pull --platform linux/386 owasp/modsecurity-crs:nginx`.
- Docker Engine 29+ needs `DOCKER_JAVA_PROPERTIES="api.version=1.44"`.
