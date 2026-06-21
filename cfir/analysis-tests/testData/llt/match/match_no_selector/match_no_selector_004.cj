


open class A {}

class B <: A {}

class C <: A {}

main(): Int64 {
    let a = A()
    var b = B()
    var c = C()
    var res: A = match {
        case false => b
        case _ => c
    }
    return 0
}
