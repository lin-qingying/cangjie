// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 接受规范等价的类型名和反引号声明的普通引用。
package café
class Café {}
func `élément`(value: Café): Café { value }
func check(value: Café): Café { return élément(value) }
