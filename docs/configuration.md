# Configuration

OAShield is configured through the OpenAPI Generator `--additional-properties`
flag. This page documents the available options, the two engine flavors, and
the known limitations of request-body validation.

## Options

| Option | Default | Description |
|---|---|---|
| `engineFlavor` | `modsecurity3` | Target engine: `modsecurity3` or `coraza` |
| `validateBodySchema` | `true` | Emit request-body validation rules |
| `generateJsonSchema` | `true` | Emit the JSON Schema file |
| `jsonSchemaOutputFile` | `schema.json` | JSON Schema file name |
| `schemaRulePath` | same as `jsonSchemaOutputFile` | Schema path written inside the `@validateSchema` rule. Coraza resolves it relative to the **server process working directory**, not the rules directory |

Pass them comma-separated:

```bash
... generate -g modsecurity3 -i api.yaml -o out \
  --additional-properties engineFlavor=coraza,schemaRulePath=rules/schema.json
```

## Engine flavors

Most generated rules are identical across both engines — the flavor only
changes how JSON request bodies are validated.

| Flavor | JSON body validation |
|---|---|
| `modsecurity3` (default) | Per-field rules generated from the OpenAPI schema: required-property presence, per-property type patterns, numeric minimum/maximum, and an `ARGS_NAMES` allowlist that rejects undeclared properties (`additionalProperties`) |
| `coraza` | The same per-field rules **plus** a `@validateSchema` rule that validates the raw body against the generated JSON Schema. `@validateSchema` is Coraza-only — ModSecurity v3's operator of the same name is XSD/XML-only |

## Limitations of per-field body validation

These apply to the `modsecurity3` flavor's per-field checks. Coraza's
`@validateSchema` additionally covers all of them:

- **Type coercion:** values are validated after the engine flattens JSON to
  strings, so a JSON number where a free-form string is expected
  (e.g. `"name": 123`) is not distinguishable.
- **`required` scope:** enforced for object properties (nested ones only when
  their parent object is present, per JSON Schema semantics), but not per array
  element. An empty array satisfies a required array property only on the
  `coraza` flavor.
- **Nesting depth:** model nesting is flattened to 5 levels; deeper properties
  are covered only by the unknown-property allowlist.
- **Numeric bounds on path parameters:** `minimum`/`maximum` on *path*
  parameters is enforced only lexically, via the embedded path pattern.
- **`oneOf` / `anyOf` composition:** enforced as the *union* of the member
  schemas. Primitive unions (including composed query/path parameters) are
  validated against an alternation of the member patterns; model unions
  validate and allowlist the properties of every branch, but `required`
  properties inside a branch are not enforced (only one branch need be
  present), and `oneOf`'s exactly-one semantics are not distinguished from
  `anyOf`. `allOf` models are validated fully (members are merged). The
  generated JSON Schema keeps the exact `oneOf`/`anyOf` keywords for
  Coraza's `@validateSchema`.
