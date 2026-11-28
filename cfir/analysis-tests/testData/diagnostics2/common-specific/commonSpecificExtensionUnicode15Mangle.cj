// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 按 Unicode 15 NFC 重排后判定 private 名称冲突。
package unicode15_mangle
common extend Int64 { private func à𞓬(): Unit {} }
common extend Int64 { <!COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS!>private func à𞓬(): Unit {}<!> }
