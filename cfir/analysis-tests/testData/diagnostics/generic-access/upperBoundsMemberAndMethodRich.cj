// RUN_PIPELINE_TILL: FRONTEND
interface Named {
    public func name(): String
}

class User <: Named {
    public override func name(): String {
        return "user"
    }
}

func ok<T>(value: T): String where T <: Named {
    return value.name()
}

func badField<T>(value: T): Int64 where T <: Named {
    return value.<!GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS!>id<!>
}

func badCall<T>(value: T): Unit where T <: Named {
    value.<!GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS!>reset<!>()
}

func useOk(): String {
    return ok(User())
}
