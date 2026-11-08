// MODULE: main
// MAIN_FILE_NAME: useCrossFileEnumConstructor.cj
// FILE: enumCtorScope.cj
package test

// 同包另一文件里的 enum：官方 `Sema/TypeCheckCall.cpp` 的 `CalculateEnumCtorScopeLevel`
// 在限定位之外按 global scope level 参与查找，因此裸枚举构造器名必须能从包级 callable 入口解析。
enum CommandType {
    | Query
    | Quit
}

// FILE: useCrossFileEnumConstructor.cj
package test

func defaultCommand(): CommandType {
    return Query
}

func fireQuit(): CommandType {
    let cmd = Quit
    return cmd
}
