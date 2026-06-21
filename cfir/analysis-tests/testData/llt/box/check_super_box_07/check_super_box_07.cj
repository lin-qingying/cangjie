// FILE: pkg.cj


package pkg

public abstract class A <: Equatable<A> {}

// FILE: check_super_box_07.cj


internal import pkg.*

class B <: A {
    public operator func ==(a: A) {
        true
    }
    public operator func !=(a: A) {
        false
    }
}

main() {
    let a: A = B()
    let v: Any = a
    let cmpRes = match (v) {
        case t: Equatable<A> => t == B()
        case _ => false
    }
    if (cmpRes) {
        return 0
    }
    return 1
}
