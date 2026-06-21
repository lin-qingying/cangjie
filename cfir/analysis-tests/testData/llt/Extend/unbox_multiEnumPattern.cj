


class B {
    func g(): Int64 {
        println("func g in B!")
        10
    }
}

interface I {
    func f(): Int64
}

extend B <: I {
    func <!WEAK_VISIBILITY!>f<!>() {
        println("In extend B!");
        1
    }
}

main(): Int64 {
    var m: E = C(B())

    var res: Int64 = match (m) {
        case C(<!VAR_IN_OR_PATTERN!>x<!>: B) | D(<!REDECLARATION!>x<!>: B) => x.f();
        case _ => 0
    }

    var n: E = D(B())

    res = match (n) {
        case C(<!VAR_IN_OR_PATTERN!>x<!>: B) | D(<!REDECLARATION!>x<!>: B) => x.g()
        case _ => 0
    }
    res
}

enum E {
    C(I) | D(I)
}
