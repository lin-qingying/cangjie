// RUN_PIPELINE_TILL: FRONTEND
// 证据：官方 cjc 1.0.5 / 1.1.3 实测（kind sema_conflict_with_sub_package）：`package conflictpkg` 下的顶层类 `child`
// 与子包 `conflictpkg.child` 同名
//   → error: top-level declaration 'child' is conflicted with possible sub-package 'conflictpkg.child'（锚声明名 `child` 整段）。
// 取证方式：包目录布局 + `cjc -p conflictpkg`；同目录双文件不同包（含加 `--no-sub-pkg`）会先报
//   package_multiple_package_declarations，取不到本诊断。
// CFIR CfirGeneralSemanticsChecker.checkConflictWithSubPackage 对存在同名子包的顶层声明报 CONFLICT_WITH_SUB_PACKAGE（锚声明）。
// FILE: pkg.cj
package conflictpkg

class <!CONFLICT_WITH_SUB_PACKAGE!>child<!> {}

// FILE: child.cj
package conflictpkg.child

class Marker {}
