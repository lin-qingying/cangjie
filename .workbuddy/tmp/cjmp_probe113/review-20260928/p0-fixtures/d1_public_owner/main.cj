package p0_d1_public_owner

public interface DefaultContract<T> {
    public func inherited(value: T): Unit {}
}

public interface Bound {}

public open class Parent<T> <: DefaultContract<T> {
    public init() {}
}

extend<T> Parent<T> {
    private func inherited(value: Int64): Unit {}
}

public open class Child<T> <: Parent<T> {
    public init() {}
}

public class InheritedDefaultOwner<T> <: Child<T> {
    public init() {}
}

extend<U> InheritedDefaultOwner<U> where U <: Bound {
    public func inherited(value: Int64): Unit {}
}

extend Int64 <: Bound {}

public func trigger(value: InheritedDefaultOwner<Int64>): Unit {}
