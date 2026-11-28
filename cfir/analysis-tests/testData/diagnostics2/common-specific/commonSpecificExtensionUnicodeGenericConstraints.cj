// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：下列三个泛型扩展均无诊断，不因相同源码拼写报告重复。
package e9_unicode_constraint
interface Bound {}
class Box<T> {}
common extend<é> Box<é> where é <: Bound { private func f(): Unit {} }
common extend<é> Box<é> where é <: Bound { private func f(): Unit {} }
common extend<é> Box<é> where é <: Bound { private func f(): Unit {} }
