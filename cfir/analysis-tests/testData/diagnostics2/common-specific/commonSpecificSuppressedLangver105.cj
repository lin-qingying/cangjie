// LANGUAGE_VERSION: 1.0.5
// RUN_PIPELINE_TILL: FRONTEND
// 官方取证：v1.0.x cjc 对 `common class` 报 parse_expected_decl（语法不存在），
// CJMP 语义检查在 1.0.x 根本不存在。CFIR 的解析器版本无关（D12），`common`/`specific`
// 照常进 PSI 树；版本门（CommonSpecificDeclarations，since 1.1.0）在 checker 层短路——
// 1.0.x 下每处 CJMP 修饰符只报版本门诊断 UNSUPPORTED_FEATURE，全部族诊断零报告。
// （同名重声明仍由通用重声明检查报告，与 CJMP 无关。）
<!UNSUPPORTED_FEATURE!>common open class A {}<!>

<!UNSUPPORTED_FEATURE!>specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}<!>
