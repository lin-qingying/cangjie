

package pkgs003001a

public func NormalFunc(a: Int32): Int32 {
    return a
}

public func Foo<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo<T, Int32>(a, b)
    return c
}
