# How It Works

OAShield turns an OpenAPI specification into a **positive security model** — a
set of WAF rules that permit exactly the requests your API declares and deny
everything else. This page explains what the generated rules actually do.

## The big picture

A conventional WAF is a *blocklist*: it inspects each request against
signatures of known attacks and blocks matches. Anything the signatures don't
recognize passes through.

OAShield builds an *allowlist* instead. Your OpenAPI spec already describes
every legitimate endpoint, method, parameter, and body field. OAShield compiles
that description into [SecLang](https://coraza.io/docs/seclang/) rules that:

1. Match the incoming request against each declared operation.
2. Validate the request's parameters and body against that operation's schema.
3. **Deny by default** if no operation matched, or if validation failed.

The result runs on any engine that speaks SecLang —
[OWASP ModSecurity v3](https://github.com/owasp-modsecurity/ModSecurity) or
[Coraza](https://coraza.io).

## Anatomy of the generated rules

OAShield emits one `.conf` file per API tag (e.g. `PetApi.conf`,
`StoreApi.conf`). Every file is a sequence of **operation blocks** followed by a
single **default-deny** at the end. The examples below are taken verbatim from
[`samples/output/petstore/`](../samples/output/petstore).

### 1. Operation matching (skip-if-not-this-operation)

Each block begins by asking "is this request for *this* operation?" If not, it
skips ahead to the block's end marker and moves on. Three checks gate the block —
path, exact-path (no extra segments), and method:

```seclang
# addPet: POST /pet
SecRule REQUEST_URI    "!@restpath /pet"        "id:4200001,phase:2,pass,nolog,skipAfter:END_addPet"
SecRule REQUEST_URI    "!@rx ^/pet(\?.*)?$"     "id:4200002,phase:2,pass,nolog,skipAfter:END_addPet"
SecRule REQUEST_METHOD "!@within POST"          "id:4200003,phase:2,pass,nolog,skipAfter:END_addPet"
```

`@restpath` matches the OpenAPI path template (including `{petId}`-style
placeholders); the `@rx` line rejects requests with extra trailing segments so
`/pet/extra` doesn't slip into the `/pet` block. If any check fails, `skipAfter`
jumps to `SecMarker END_addPet` — this request isn't for `addPet`.

### 2. Parameter validation

Once a request is confirmed to belong to an operation, its parameters are
checked against the schema. Values that don't match are denied outright; a
`skipAfter:FAILED_API_CHECKS` sends them to the default-deny at the end of the
file:

```seclang
# path parameter petId, typed as integer
SecRule  ARGS_PATH:petId "!@rx ^[0-9]{1,19}$" "id:4210021,phase:2,deny,status:403,msg:'Forbidden parameter value detected',...,skipAfter:FAILED_API_CHECKS"
SecRule &ARGS_PATH:petId "@gt 1"              "id:4210022,phase:2,deny,status:403,msg:'Multiple values for non-array parameter',...,skipAfter:FAILED_API_CHECKS"

# enum query parameter status
SecRule ARGS_GET:status "!@rx ^(available|pending|sold)$" "id:4210063,phase:2,deny,status:403,...,skipAfter:FAILED_API_CHECKS"
```

Each parameter contributes two kinds of rule: a **value** check (`@rx` derived
from the schema's type, enum, pattern, or numeric bounds) and a **cardinality**
check (`&ARGS…` counts occurrences, enforcing that a non-array parameter appears
exactly once).

### 3. Unknown-parameter allowlist

After the declared parameters are validated, anything *not* declared is
rejected. The allowlist regex lists the permitted names; a request carrying any
other parameter is blocked:

```seclang
# operation with no query params — any query arg is unknown
SecRule ARGS_GET_NAMES "@rx ^.+$"          "id:4200007,phase:2,block,msg:'Unknown parameter detected',...,skipAfter:FAILED_API_CHECKS"

# operation that declares `status` — anything else is unknown
SecRule ARGS_GET_NAMES "!@rx ^(status)$"   "id:4200046,phase:2,block,msg:'Unknown parameter detected',...,skipAfter:FAILED_API_CHECKS"
```

### 4. Operation passed — allow and exit the block

If the request survived every check, it's a valid call to this operation. The
block allows it and closes with its end marker:

```seclang
SecAction "phase:2,allow:request,id:4200008"
SecMarker END_addPet
```

### 5. Default deny

At the very end of the file sits the target of every `FAILED_API_CHECKS` jump,
and the fall-through for any request that matched no operation at all:

```seclang
# For anything else, deny by default
SecMarker FAILED_API_CHECKS
SecAction "id:4220001,log,auditlog,block,phase:2,msg:'Unknown API endpoint'"
```

This is what makes the model *positive*: reaching the end of the file without an
explicit `allow` means the request is denied.

## Request-body validation

Parameters cover the query string, path, and form fields. JSON request bodies
are handled separately, and this is where the two [engine flavors](configuration.md)
differ:

- **`modsecurity3` (default)** — per-field rules generated from the schema:
  required-property presence, per-property type patterns, numeric
  `minimum`/`maximum`, and an `ARGS_NAMES` allowlist that rejects undeclared
  properties. This works on both engines because it uses only standard operators.

- **`coraza`** — everything above **plus** a `@validateSchema` rule that
  validates the raw body against a generated JSON Schema file. `@validateSchema`
  is Coraza-only; ModSecurity v3's operator of the same name handles XSD/XML,
  not JSON.

Because the per-field approach runs *after* the engine flattens JSON into
string parameters, it has some inherent limits (type coercion, array-element
`required`, deep nesting). Those are enumerated in
[Configuration → Limitations](configuration.md#limitations-of-per-field-body-validation),
and are exactly the cases Coraza's `@validateSchema` covers in full.

## Why per-tag files and phase 2?

- **Phase 2** is ModSecurity's request-body phase — by then the URI, method,
  query args, and body are all available, so a single pass can validate the
  whole request.
- **One file per tag** keeps the rules readable and lets you deploy or review
  subsets of your API independently. Load them all together and the final
  default-deny in each still applies to its own operations.

## See it yourself

The fastest way to build intuition is to generate rules for a spec you know and
read the output:

```bash
java -cp target/oashield-cli.jar org.openapitools.codegen.OpenAPIGenerator \
  generate -g modsecurity3 -i samples/petstore.yaml -o output/
```

Then open `output/PetApi.conf` and follow the blocks top to bottom. The
[Petstore sample output](../samples/output/petstore) is already checked in if
you'd rather just browse.
