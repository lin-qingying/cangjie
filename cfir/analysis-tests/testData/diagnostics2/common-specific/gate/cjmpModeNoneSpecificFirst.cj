// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// 模式门反例（计划 §8.4）：同上，specific 在前时报 specific 方向诊断（cjc 1.1.3 实测 none3.cj）。
// 文件角色错误由共享 parser 报告，CFIR fixture 不登记语法工厂。
package cjmp_p

public specific func b(): Int64 { 1 }
public common func a(): Int64 { 1 }
