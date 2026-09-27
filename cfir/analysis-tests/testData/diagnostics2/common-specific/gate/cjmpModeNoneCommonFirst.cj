// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 模式门反例（计划 §8.4）：既非 common 也非 specific 编译（无 CJMP_MODE）。
// cjc 1.1.3 实测（`cjc none2.cj --experimental`）：只报一条文件级诊断，锚 package 行 1:1，
// 取源码顺序上首个错位声明（此处 common 在前）；其余 CJMP 规则不执行。
<!PARSE_COMMON_IN_NON_COMMON_FILE!>package cjmp_p<!>

public common func a(): Int64 { 1 }
public specific func b(): Int64 { 1 }
