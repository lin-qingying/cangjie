// MODULE: dependency
// FILE: dependency.cj
package index.dependency

public func indexedLoad(): Int64 { 1 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 依赖模块是**源码**模块：这个名字既不在导入作用域里，也不在任何作用域链上。
// 宿主没有名称索引时补全不得给出它——给出即意味着候选来自遍历源码文件的兜底路径。
func indexSite(): Int64 {
    indexedLoad
}
