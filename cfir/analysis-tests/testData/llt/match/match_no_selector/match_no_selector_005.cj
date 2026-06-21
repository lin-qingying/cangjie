

open class A {}

class B <: A {}

class C <: A {}

main(): Int64 {
    let a = A()
    var b = B()
    var c = C()
    var res: B = match {
        case false => <!TYPE_MISMATCH!>a<!>
        case _ => <!TYPE_MISMATCH!>c<!>
    }
    return 0
}
