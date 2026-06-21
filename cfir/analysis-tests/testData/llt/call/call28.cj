

internal import std.core.Object

public interface Comparables<Y> {}

public class Integer <: Comparables<Integer> {
    public init(val: Int64) {}
}

public interface InteropTest<Z> where Z <: Comparables<Z> {
    func test(value1: Z, value2: Z): Unit {}
}

class Child<T> <: InteropTest<T> where T <: Comparables<T> {}

main() {
    var interoptest: Child<Integer> = Child<Integer>()
    interoptest.test(Integer(-12345), Integer(12345))
    return 0
}
