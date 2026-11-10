// MODULE: dependency
// FILE: dependency.cj
package index.dependency

public func shadowedByFile(): Int64 { 1 }

// MODULE: main(dependency)
// MAIN_MODULE
// FILE: main.cj
package completion.sections

// 本文件自己声明了同名函数：索引里的依赖声明被它遮蔽，不得作为候选存活。
public func shadowedByFile(): Int64 { 2 }

func indexSite(): Int64 {
    shadowedByFile
}
