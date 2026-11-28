// MODULE: main
// MAIN_FILE_NAME: recursiveConstructorLazySuperConstructor.cj
// FILE: lazySuperBase.cj
package test

// 父类在**另一个文件**里：逐文件分析时它的构造器体仍是 lazy body。
// 递归构造器检查沿 `super(...)` 走到这里时必须跳过未解析的体，而不是读
// `CfirLazyBlock.statements` 抛异常（真实项目 `server_auth_more_data.cj` 即此形态）。
open class LazySuperBase {
    init() {
    }
}

// FILE: recursiveConstructorLazySuperConstructor.cj
package test

class LazySuperDerived <: LazySuperBase {
    init() {
        super()
    }
}
