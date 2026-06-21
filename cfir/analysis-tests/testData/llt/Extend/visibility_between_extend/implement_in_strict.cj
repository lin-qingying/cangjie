


interface I {
    func f(): Unit
}

open class C1 {}

class C2 <: C1 {}

class A<T> {}

extend<T> A<T> where T <: C2 {
    public func f() {}
}

<!NEED_MEMBER_IMPLEMENTATION!>e<!>xtend<T> A<T> <: I where T <: C1 {}
