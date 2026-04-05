// FILE: memberScopeQueries.cjs
// TARGET_CLASS: ScriptUser
// DECLARED_MEMBER_SCOPE_AVAILABLE_NAME: rename
// DECLARED_MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_AVAILABLE_NAME: rename
// MEMBER_SCOPE_AVAILABLE_NAME: inherited
// MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_CALLABLE: inherited

package sample.scope.script

open class ScriptBase {
    public func inherited(): Int64 {
        return 1
    }
}

class ScriptUser <: ScriptBase {
    public func rename(): Int64 {
        return inherited()
    }
}
