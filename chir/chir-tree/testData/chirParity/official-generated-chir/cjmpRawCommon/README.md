# Official v1.1.3 raw common CHIR fixture

`cjmp_raw_chir_v113.chir` and `cjmp_raw_chir_v113.cjo` are emitted together by
the official Cangjie 1.1.3 compiler from `common.cj`:

```powershell
cjc common.cj --experimental --output-type=chir --output-dir . --diagnostic-format=json
```

The official `ToCHIR::Run` path serializes `OutputMode::CHIR` packages with
`Phase::RAW`; this fixture therefore records the positional common-part input
shape used by the legacy CJMP driver. Do not regenerate it with
`--emit-chir=raw`, which selects a CHIR dump phase without selecting the
common-part output mode.
