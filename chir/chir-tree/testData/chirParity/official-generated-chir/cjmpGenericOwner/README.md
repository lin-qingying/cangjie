# Official v1.1.3 generic nominal CJMP fixture

`cjmp_generic_owner_v113.chir` and `cjmp_generic_owner_v113.cjo` were emitted together from `common.cj` by the official Cangjie 1.1.3 compiler:

```powershell
cjc common.cj --experimental --output-type=chir --output-dir . --diagnostic-format=json
```

`specific.cj` is the matching source input used by the repository's production CLI test. It renames the common interface type parameter `T` to `U`, exercising the loaded generic nominal identity and preserving the common generic member under owner type-parameter actualization.
