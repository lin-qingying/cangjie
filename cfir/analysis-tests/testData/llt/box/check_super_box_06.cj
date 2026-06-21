

abstract class A <: ToString {}

class B <: A {
    public func toString(): String {
        return "B"
    }
}

main() {
    let a: A = B()
    let v: Any = a
    let str = match (v) {
        case t: ToString => t.toString()
        case _ => ""
    }
    if (str != "B") {
        return 1
    }
    return 0
}
