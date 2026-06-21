

open class Father {}

class Son <: Father {
    public var one: Int64 = 2
}

main(): Int64 {
    var father = Father()
    var res = match (father) {
        case son: Son => son.one
        case _ => 1
    }
    return Int64(res - 1)
}
