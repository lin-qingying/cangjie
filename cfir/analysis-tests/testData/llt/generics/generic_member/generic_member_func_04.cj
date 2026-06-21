


open class A<X> {
    public func test<T>(x: X): X {
        println("a");
        x
    }
    public static func goo<Y>(x: Y): Y {
        println("a");
        x
    }
}

class B<X> <: A<X> {
    public func test<Y>(x: Y): Y {
        println("b");
        x
    }
    public static redef func goo<T>(x: T): T {
        println("b");
        x
    }
}

open class AA {
    public func test<T1, T2>(x: T1, y: T2): T1 {
        println("a");
        x
    }
}

class BB <: AA {
    public func test<T>(x: T, y: T): T {
        println("b");
        x
    }
}

main() {
    0
}
