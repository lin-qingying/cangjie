


interface A {
    func default_foo(): Int32 {
        1
    }
}

interface B<X> <: A {
    func default_foo(): Int32 {
        2
    }
}

class C <: B<Int32> {}

main() {
    var b: B<Int32> = C()
    print(b.default_foo().toString())
    if (b.default_foo() != Int32(2)) {
        print("default_foo failed!")
        return 1
    }
    return 0
}
