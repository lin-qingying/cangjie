



interface I1 {
    mut static prop a: Int64
    static func f(): Unit
}

interface I2 <: I1 {}

abstract class I3 {}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend I3 <: I2 {}

func f<T>(): Unit where T <: I2 {
    T.a = 1
    T.f()
}

main() {
    f<I3>()
}

