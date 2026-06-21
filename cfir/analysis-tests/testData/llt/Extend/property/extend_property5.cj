

class A {}

interface Foo {
    prop v1: Int64
}

extend A <: Foo {
    public prop v1: Int64 {
        get() {
            3
        }
    }
}

class A1 {
    public prop v1: Int64 {
        get() {
            5
        }
    }
}

interface Foo1 {
    prop v1: Int64
}

extend A1 <: Foo1 {}

main() {
    0
}
