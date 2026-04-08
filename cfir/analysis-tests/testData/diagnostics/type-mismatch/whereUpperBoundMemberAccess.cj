// RUN_PIPELINE_TILL: FRONTEND
interface Named {
    func name(): String
}

class User <: Named {
    public func name(): String {
        return "user"
    }
}

func readName<T>(value: T): String where T <: Named {
    return value.name()
}

func useReadName(): String {
    return readName(User())
}
