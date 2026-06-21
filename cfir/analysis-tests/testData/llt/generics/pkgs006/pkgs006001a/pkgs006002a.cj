

package pkgs006001a

public func Bar<T>(a: T) {
    var b: Int32 = NormalFunc(1)
    var c = Foo<T, Int32>(a, b)
    return c
}
