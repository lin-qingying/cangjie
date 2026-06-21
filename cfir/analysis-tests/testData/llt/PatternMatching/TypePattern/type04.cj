

open class Father {
    public var age = 40
}

class Son <: Father {
    public var one: Int64 = 2
    public init() {
        age = 20
    }
}

main(): Int64 {
    var father = Father()
    var res = match (father) {
        case son: Son => son.one
        case _ => 1
    }
    if (res == 1) {
        return 0
    }
    return 1
}
