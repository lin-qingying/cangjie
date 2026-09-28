// RUN_PIPELINE_TILL: FRONTEND

class PrivateOwnerCollision<T> {
    init() {}

    public func f(value: Int64): Unit {}
}

extend<T> PrivateOwnerCollision<T> {
    private func f(value: T): Unit {}
}

class UnrelatedExtendOwners<T> {
    init() {}
}

extend<T> UnrelatedExtendOwners<T> {
    private func f(value: T): Unit {}
}

extend<T> UnrelatedExtendOwners<T> {
    public func f(value: Int64): Unit {}
}

interface DefaultContract<T> {
    public func inherited(value: T): Unit { }
}

interface Bound { }

open class Parent<T> <: DefaultContract<T> {
    init() { }
}

extend<T> Parent<T> {
    private func inherited(value: Int64): Unit { }
}

// The private Parent extension must not shadow DefaultContract's inherited default before owner filtering.
open class Child<T> <: Parent<T> {
    init() { }
}

class InheritedDefaultOwner<T> <: Child<T> {
    init() { }
}

extend<U> InheritedDefaultOwner<U> where U <: Bound {
    private func inherited(value: Int64): Unit { }
}

extend Int64 <: Bound { }

func instantiate(): Unit {
    let collision = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateOwnerCollision<Int64><!>()
    let isolated = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>UnrelatedExtendOwners<Int64><!>()
    let inheritedDefault = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>InheritedDefaultOwner<Int64><!>()
}
