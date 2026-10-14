<!MISSING_ENTRY!>// 证据：官方 cjc 1.0.0 / 1.1.3 在可执行目标下对无 `main` 的源码报 kind sema_missing_entry
//   "error: 'main' is missing"，官方位置是文件首字符 1:1；CFIR 使用 CfirFile.source，故标记覆盖整个文件。
//   官方只在可执行目标下检查入口：`--output-type=staticlib` 编译无 main 的文件零诊断。
//   合法对照：带 `main(): Int64` 的文件零诊断。指令必须写在首个 FILE: 段之前。
// RUN_PIPELINE_TILL: FRONTEND
// CHECK_PROGRAM_ENTRY
func helper(): Int64 {
    return 1
}<!>
