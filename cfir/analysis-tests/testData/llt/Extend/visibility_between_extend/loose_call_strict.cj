


interface I1 {}

interface I2 <: I1 {}

open class C1 <: I2 {
    static var i = 1
}

class C2 <: C1 {}

class A<T, R> {}

extend<T, R> A<T, R> where T <: C1, R <: I2 {
    func f() {
        return T.i
    }
}

extend<R, T> A<R, T> where T <: I1, R <: C2 {
    func f1() {
        return <!UNRESOLVED_REFERENCE!>f<!>()
    }
}

extend<R, T> A<R, T> where T <: I2, R <: I2 {
    func f2() {
        return <!UNRESOLVED_REFERENCE!>f<!>()
    }
}

extend<R, T> A<R, T> where T <: I1, R <: I2 {
    func f3() {
        return <!UNRESOLVED_REFERENCE!>f<!>()
    }
}
