


public open class Parent {
    public static func test(): Int64 {
        2
    }
}

public class Child <: Parent {
    public static redef func test(): Int64 {
        3
    }
}

public func test<T>() where T <: Child {
    T.test()
}

public func test2<T>() where T <: Parent {
    T.test()
}

main(): Int64 {
    let v1 = test<Child>()
    let v2 = test2<Child>()
    if (v1 != 3) {
        return 1
    }
    if (v2 != 3) {
        return 2
    }
    return 0
}
