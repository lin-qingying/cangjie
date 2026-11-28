// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：命名参数的默认值存在性参与原始名称，默认值内容不参与。
package e9_named_default
common extend Int64 { private func f(value!: Int64 = 1): Unit {} }
common extend Int64 { private func f(value!: Int64): Unit {} }
common extend Int64 { <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(value!: Int64 = 2): Unit {}<!> }
