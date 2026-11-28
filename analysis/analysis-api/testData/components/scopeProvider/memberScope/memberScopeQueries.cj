// FILE: memberScopeQueries.cj
// TARGET_CLASS: User
// DECLARED_MEMBER_SCOPE_AVAILABLE_NAME: rename
// DECLARED_MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_AVAILABLE_NAME: rename
// MEMBER_SCOPE_AVAILABLE_NAME: inherited
// MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_CALLABLE: inherited
// DECLARED_MEMBER_SCOPE_AVAILABLE_NAME: field
// DECLARED_MEMBER_SCOPE_AVAILABLE_NAME: state
// DECLARED_MEMBER_SCOPE_CALLABLE: field
// DECLARED_MEMBER_SCOPE_CALLABLE: state

package sample.scope

open class Base {
    public let state: Int64 = 0

    public func inherited(): Int64 {
        return 1
    }
}

class User <: Base {
    // 字段与成员变量必须出现在声明成员作用域中：它们走 `processVariablesByName`，
    // 与函数走 `processFunctionsByName`，两者都不可缺失。
    //
    // use-site member scope 暂不在此处断言字段：`CaCfirFieldSymbol.createPointer`
    // 尚未实现稳定指针，渲染路径会直接报错。补齐指针后应把 field/state 同时
    // 加入 MEMBER_SCOPE_* 指令。
    public var field: Int64 = 0

    public func rename(): Int64 {
        return inherited()
    }
}
