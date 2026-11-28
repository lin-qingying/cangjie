// LANGUAGE_VERSION: 1.0.5
// RUN_PIPELINE_TILL: FRONTEND
// 旧语言版本由 CJMP feature gate 报 UNSUPPORTED_FEATURE；同名重声明仍由通用 checker 报告。
<!UNSUPPORTED_FEATURE!>common open class A {}

<!><!UNSUPPORTED_FEATURE!>specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}<!>
