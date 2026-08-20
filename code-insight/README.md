# Code insight

The `code-insight/` modules provide editor-facing language services shared by IDE and language-service integrations. They depend on Analysis API contracts and IntelliJ platform APIs; they do not own parsing or CFIR semantic resolution.

| Module | Responsibility |
| --- | --- |
| `:code-insight` | Aggregates the code-insight modules |
| `:code-insight:api` | Editor-service and quick-fix API contracts |
| `:code-insight:fixes` | Quick-fix registrations and implementations |
| `:code-insight:formatting` | Formatter and code-style integration |
| `:code-insight:folding` | Folding-region collection |
| `:code-insight:highlighting` | Lexical and structural highlighting |
| `:code-insight:override-implement` | Override and implement-member actions |
| `:code-insight:refactoring` | Rename and refactoring support |

## Build and test

```powershell
.\gradlew.bat :code-insight:assemble
.\gradlew.bat :code-insight:formatting:test
.\gradlew.bat :code-insight:folding:test
.\gradlew.bat :code-insight:highlighting:test
```

## Related documentation

- [Analysis subsystem](../analysis/README.md)
- [IntelliJ Platform plugin](../intellij-ide/README.md)
- [Module catalog](../docs/module-catalog.md)
