// LANGUAGE_VERSION: 1.1.0
// LANGUAGE: -CommonSpecificDeclarations
// CJMP_MODE: SPECIFIC
// RUN_PIPELINE_TILL: FRONTEND
// 特性覆盖反例（计划 §8.4）：1.1.0 下显式关闭 CommonSpecificDeclarations，行为与版本门反例一致——
// 每个 CJMP 修饰符只报版本门诊断，模式门 / 配对 / 族诊断零报告（门禁管行为：配对阶段早退）。
package cjmp_p

<!UNSUPPORTED_FEATURE!>public specific func b(): Int64 { 1 }<!>
