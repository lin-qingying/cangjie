// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 使用非法继承类型的原始拼写编码 private 成员。
package e9_alias_super
class Plain {}
type Alias = Plain
common extend Int64 <: <!EXTEND_NOT_INTERFACE!>Plain<!> { private func f(): Unit {} }
common extend Int64 <: <!EXTEND_NOT_INTERFACE!>Alias<!> { private func f(): Unit {} }
common extend Int64 <: <!EXTEND_NOT_INTERFACE!>Plain<!> { <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(): Unit {}<!> }
