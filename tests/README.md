# Test infrastructure

The `tests/` namespace owns reusable compiler test infrastructure. The aggregation module `:tests` has no production sources; `:tests:test-infrastructure` exposes its test framework through Gradle test fixtures and the `prepare` publication facade.

| Module | Responsibility |
| --- | --- |
| `:tests` | Aggregates shared test infrastructure |
| `:tests:test-infrastructure` | Test runners, directives, services, models, facades, and inline diagnostic rendering |

## Build and test

```powershell
.\gradlew.bat :tests:test-infrastructure:assemble
.\gradlew.bat :tests:test-infrastructure:test
```

Use the test fixtures from downstream modules:

```kotlin
testImplementation(testFixtures(project(":tests:test-infrastructure")))
```

## Related documentation

- [Test infrastructure module](test-infrastructure/README.md)
- [Testing conventions](../TESTING_CONVENTIONS.md)
- [Published test infrastructure](../prepare/README.md)
