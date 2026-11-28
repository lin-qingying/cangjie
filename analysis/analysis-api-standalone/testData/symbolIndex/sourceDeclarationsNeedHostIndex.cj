// MODULE: dependency
// FILE: dependency.cj
package sourceindex.dependency

public func sourceOnlyLoad(): Int64 { 1 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package sourceindex

// 依赖模块是**源码**模块：没有宿主名称索引时，这个名字不得被发现。
func sourceSite(): Int64 {
    sourceOnlyLoad
}
