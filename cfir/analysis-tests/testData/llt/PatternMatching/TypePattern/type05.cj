

open class Father {
    public var age = 40
}

class Son <: Father {
    public var one: Int32 = 2
    public init() {
        age = 20
    }
}

class Uncle {}

main(): Int64 {
    var son = Son()
    var res = match (son) {
        case uncle: Uncle => 0
        case father: Father => father.age
        case <!UNREACHABLE_PATTERN!>_<!> => 1
    }
    if (res == 20) {
        return 0
    }
    return 1
}
