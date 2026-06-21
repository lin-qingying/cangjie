


abstract class A {
    public prop p: Int64
}

class B <: A {
    public prop p: Int64 {
        get() {
            111
        }
    }
}

class C {
    private let aa: A
    init(a: A) {
        aa = a
    }

    prop p: Int64 {
        get() {
            aa.p
        }
    }
}

main(): Int64 {
    let b = B()
    let val = C(b)
    if (val.p != 111) {
        return 1
    }
    return 0
}
