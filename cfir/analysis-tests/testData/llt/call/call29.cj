

internal import std.core.Object

public open class A {}

public class Integer <: A {
    public init(val: Int64) {}
}

public interface InteropTest<Z> {
    func get(value: Z): A
}

class Child<T> <: InteropTest<T> where T <: A {
    public func get(value: T): T {
        return value
    }
}

main() {
    var interoptest: Child<Integer> = Child<Integer>()
    interoptest.get(Integer(-12345))
    return 0
}
