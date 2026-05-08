// RUN_PIPELINE_TILL: FRONTEND
interface Named {
    func name(): String
}

interface Tagged {
    func tag(): String
}

class TaggedUser <: Named & Tagged {
    public func name(): String {
        return "tagged-user"
    }

    public func tag(): String {
        return "demo"
    }
}

func describe<T>(value: T): String where T <: Named & Tagged {
    let displayName = value.name()
    value.tag()
    return displayName
}

func useDescribe(): String {
    return describe(TaggedUser())
}
