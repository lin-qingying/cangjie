# CHIR Test Fixtures

This directory follows CHIR-specific test conventions:

- Prefer reusable fixtures from `core/testkit/ChirTestFixtures.kt`.
- Prefer reusable assertions from `core/testkit/ChirTestAssertions.kt`.
- Keep each test focused on one behavior (validator, printer, codec, pipeline).
- For snapshot-like checks, use deterministic outputs (`ChirPrinter`, `ChirInspector`).

Use direct in-test builders only when a case requires highly specialized graph shape.
