# CJO SDK Fixtures

These `.cjo` files are copied from the local Cangjie SDK to validate real-world deserialization compatibility in integration tests.

Source SDK path:

`C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\modules\windows_x86_64_cjnative`

Copied fixtures:

- `windows_x86_64_cjnative/std.cjo`
- `windows_x86_64_cjnative/std/std.objectpool.cjo`

Additional `.cj.d` sidecar (plain-text declaration source, merged by the cjd
sidecar pipeline to provide cross-package annotations such as `@!APILevel`):

- `windows_x86_64_cjnative/std/std.argopt.cj.d` — official harmonyos
  build-tools source text, carries `@!APILevel[since: "22"]` on `ArgOpt`.
  Sidecar pairing rule: same directory, same file stem as the `.cjo`
  (`CjdSidecarLocator`). See commit `698a8e7ea`.

Refresh command (PowerShell):

```powershell
Copy-Item "C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\modules\windows_x86_64_cjnative\std.cjo" `
  "D:\code\intellij\cangjie\cfir\cfir-serialization\testResources\cjo-sdk\windows_x86_64_cjnative\std.cjo" -Force

Copy-Item "C:\Users\lin17\.cangjie\sdks\cangjie-1.0.5\modules\windows_x86_64_cjnative\std\std.objectpool.cjo" `
  "D:\code\intellij\cangjie\cfir\cfir-serialization\testResources\cjo-sdk\windows_x86_64_cjnative\std\std.objectpool.cjo" -Force
```
