// FILE: memberScopeQueries.cj
// TARGET_CLASS: User
// DECLARED_MEMBER_SCOPE_AVAILABLE_NAME: rename
// DECLARED_MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_AVAILABLE_NAME: rename
// MEMBER_SCOPE_AVAILABLE_NAME: inherited
// MEMBER_SCOPE_CALLABLE: rename
// MEMBER_SCOPE_CALLABLE: inherited

package sample.scope

open class Base {
    public func inherited(): Int64 {
        return 1
    }
}

class User <: Base {
    public func rename(): Int64 {
        return inherited()
    }
}
