// MODULE: unrelated
// FILE: unrelated.cj
package index.unrelated

func unrelatedCandidate(): Int64 { 0 }

// MODULE: dependency
// FILE: dependency.cj
package index.dependency

public func indexedLoad(): Int64 { 1 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 未导入的依赖模块声明：索引段必须召回，并且要求新增 import。
func indexSite(): Int64 {
    indexedLoad
}
