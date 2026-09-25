// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 模式门反例（计划 §8.4）：同上，specific 在前时报 specific 方向诊断（cjc 1.1.3 实测 none3.cj）。
<!PARSE_SPECIFIC_IN_NON_SPECIFIC_FILE!>package cjmp_p<!>

public specific func b(): Int64 { 1 }
public common func a(): Int64 { 1 }
