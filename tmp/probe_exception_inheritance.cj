class NoInitEx <: Exception {}

class ExplicitInitEx <: Exception {
    public init() {
        super()
    }
}

open class OpenNoInitEx <: Exception {}

func useAll(): Unit {
    throw NoInitEx()
    throw ExplicitInitEx()
    throw OpenNoInitEx()
}

main(): Int64 {
    return 0
}
