

package pkgs005001a

public func Foo<T>(a: T): T {
    return a
}

public func Foo2<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo2<T, Int32>(a, b)
    return c
}
