

struct B {
    var y: Int64 = 1
    mut func changeY(): Unit {
        y = 2
    }
}

struct A {
    var inner: B = B()
    func f2(): Unit { // ok, `mut` isn't required
        <!CANNOT_MODIFY_VAR!>inner<!>.changeY() // (f2 mutates B) => (f2 mutates A), but f2 is not 'mut'
    }
}

main() {
    let x = A()
    println(x.inner.y)
    x.f2()
    println(x.inner.y)
}
