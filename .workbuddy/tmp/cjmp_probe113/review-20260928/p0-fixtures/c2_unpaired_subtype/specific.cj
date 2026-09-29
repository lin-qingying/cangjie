package p0_c2_unpaired_subtype

public specific interface I<U> {}

public class D<U> <: I<U> {
    public init() {}
    public func f(value: Int64): Unit {}
}

public func trigger(value: D<Int64>): Unit {}
