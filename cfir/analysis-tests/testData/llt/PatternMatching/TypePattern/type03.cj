

open class Father {
    public var age = 40
}

class Son <: Father {
    public var one: Int32 = 2
    public init() {
        age = 20
    }
}

main(): Int64 {
    var son = Son()
    var res = match (son) {
        case father: Father => father.age
        case _ => 1
    }
    if (res == 20) {
        return 0
    }
    return 1
}
