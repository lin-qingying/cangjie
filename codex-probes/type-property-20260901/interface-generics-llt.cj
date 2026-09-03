class C {}
class D {}
class E {}

interface I0 {
    func foo(): C
}

interface I1 {
    func foo(): D
}

interface I2 {
    func foo(): E
}

interface II <: I0 & I1 & I2 {}

interface K0 {
    func test() {}
}

interface K1 {
    prop test: Int64 {
        get() { 1 }
    }
}

class A <: K0 & K1 {}

open class PropertyBase {}
class PropertyChild <: PropertyBase {}

interface P0 {
    prop value: PropertyBase {
        get() { PropertyBase() }
    }
}

interface P1 {
    prop value: PropertyChild {
        get() { PropertyChild() }
    }
}

class P <: P0 & P1 {}

interface Bar {
    func f(): String
}

struct Foo <: Bar {
    func f(): String { "A" }
}

open class Base {
    public open func f(): Foo { Foo() }
}

class Derived <: Base {
    public override func f(): Bar { Foo() }
}

func makeFoo(): Foo { Foo() }

main() {
    var tuple: (Bar, Bar) = (Foo(), Foo())
    let concrete = (Foo(), Foo())
    tuple = concrete
    var functionValue: () -> Bar = makeFoo
    0
}
