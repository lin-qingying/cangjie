

class Base {
    public func hello() {
        print("hello")
    }
}

class A {
    public static func test<T>(a: T) where T <: Base {
        a.hello()
    }
}

main(): Int64 {
    A.test<Base>(Base());
    return 0;
}
