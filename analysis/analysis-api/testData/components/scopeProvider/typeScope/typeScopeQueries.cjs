// FILE: typeScopeQueries.cjs
// TARGET_CALL: buildUser()
// TYPE_SCOPE_AVAILABLE_NAME: rename
// TYPE_SCOPE_AVAILABLE_NAME: inherited
// TYPE_SCOPE_CALLABLE: rename
// TYPE_SCOPE_CALLABLE: inherited

package sample.scope.script

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

func buildUser(): User {
    return User()
}

func consume(): Int64 {
    return buildUser().rename()
}
