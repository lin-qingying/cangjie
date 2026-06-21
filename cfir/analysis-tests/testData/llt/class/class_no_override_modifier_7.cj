

class C1<T> {}

class C2<T> {}

interface A {
    func foo(a: C1<Int32>): Int32 {
        return 0
    }
}

interface B {
    func foo(a: C2<Int32>): Int32 {
        return 1
    }
}

class D <: A & B {}

main() {
    var obj = D()
    if (obj.foo(C2<Int32>()) == 1) {
        return 0
    }
    return 1
}
