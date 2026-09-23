// LANGUAGE_VERSION: 1.0.5
// RUN_PIPELINE_TILL: FRONTEND
// 官方取证：v1.0.x cjc 对 `common class` 报 parse_expected_decl（语法不存在），
// CJMP 语义检查在 1.0.x 根本不存在。CFIR 的解析器仍然接受该语法，因此
// CommonSpecificDeclarations 门禁（since 1.1.0）必须在 1.0.x 下整族跳过——
// 本文件期望零 CJMP/SPECIFIC 诊断。
common open class A {}

specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}
