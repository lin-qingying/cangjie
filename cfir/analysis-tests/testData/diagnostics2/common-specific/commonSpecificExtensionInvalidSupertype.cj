// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: 无效接口不与无接口扩展判重；两个无接口扩展之间仍判重。
package e9_unknown_super
common extend Int64 <: <!EXTEND_NOT_INTERFACE, UNDECLARED_TYPE_NAME!>Missing<!> { private func f(): Unit {} }
common extend Int64 { private func f(): Unit {} }
common extend Int64 { <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func f(): Unit {}<!> }

class Plain {}
common extend Int64 <: <!EXTEND_NOT_INTERFACE!>Plain<!> { private func g(): Unit {} }
common extend Int64 { private func g(): Unit {} }
common extend Int64 { <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func g(): Unit {}<!> }
