

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
    var target = (Father(), Son(), Uncle())
    var res = match (target) {
        case (father: Father, son: Father, uncle: Uncle) => son.age
        case <!UNREACHABLE_PATTERN!>_<!> => -1
    }
    if (res == 20) {
        return 0
    }
    return 1
}
