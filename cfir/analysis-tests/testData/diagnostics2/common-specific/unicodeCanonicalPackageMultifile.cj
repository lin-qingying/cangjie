// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 对同一包的规范等价名称跨文件解析成功。
// FILE: declarations.cj
package café
public class Élément {}
public func `référer`(value: Élément): Élément { value }

// FILE: references.cj
package café
func check(value: Élément): Élément { référer(value) }
