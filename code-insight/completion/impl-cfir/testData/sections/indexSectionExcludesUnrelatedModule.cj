// MODULE: unrelated
// FILE: unrelated.cj
package index.unrelated

public func unrelatedCandidate(): Int64 { 0 }

// MODULE: main
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 与 unrelated 模块没有依赖关系：索引即使能看到名字也不得召回。
func indexSite(): Int64 {
    unrelatedCandidate
}
