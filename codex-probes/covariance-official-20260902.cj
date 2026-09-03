interface Bar {
    func f(): String
}

struct Foo <: Bar {
    func f(): String {
        "A"
    }
}

open class A {
    public open func f(): Foo {
        return Foo()
    }
}

class B <: A {
    public override func f(): Bar {
        return Foo()
    }
}

func f(): Foo {
    return Foo()
}

main() {
    var a: (Bar, Bar) = (Foo(), Foo())
    let b = (Foo(), Foo())
    a = b
    var c: () -> Bar = f
    return 0
}
