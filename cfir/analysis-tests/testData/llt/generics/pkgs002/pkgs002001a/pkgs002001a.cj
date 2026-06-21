

package pkgs002001a

public func Foo<T, U>(a: T, b: U): T {
    return a
}

public func Bar<T>(a: T) {
    var b: Int32 = 1
    var c = Foo<T, Int32>(a, b)
    return c
}
