package p0_d1_owner_interface

public interface J<T> {
    public func f(value: T): Unit {}
}

public interface Bound {}

public class X<T> {
    public init() {}
}

extend<U> X<U> <: J<U> where U <: Bound {
    public func f(value: Int64): Unit {}
}

extend Int64 <: Bound {}

public func trigger(value: X<Int64>): Unit {}
