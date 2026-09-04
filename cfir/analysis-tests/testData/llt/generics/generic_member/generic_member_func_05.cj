


open class A<X> {
    public static func test<T>(x: X): X {
        println("a");
        x
    }
    public func goo<T>(x: T): T {
        println("a");
        x
    }
}

class B<X> <: A<X> {
    public static <!NOTHING_TO_OVERRIDE!>redef<!> func test<T>(x: T): T {
        println("b");
        x
    }
    public func <!CANNOT_OVERRIDE!>goo<!><Y>(x: Y): Y {
        println("b");
        x
    }
}

main() {
    0
}
