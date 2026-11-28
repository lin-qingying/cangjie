// MODULE: dependency
// FILE: dependency.cj
package index.dependency

public func indexedOverload(): Int64 { 1 }
public func indexedOverload(value: Int64): Int64 { value }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 未导入的依赖模块里有同名重载：索引按名称召回一次，恢复必须展开成两个候选。
func indexSite(): Int64 {
    indexedOverload
}
