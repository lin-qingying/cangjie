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
    <!REDUNDANT_MODIFIER_FOR_TARGET!>public<!> func inherited(value: T): Unit { }
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

class Leaf<T> <: Child<T> {
    init() { }

    public func inherited(value: Int64): Unit { }
}

class InheritedDefaultOwner<T> <: Child<T> {
    init() { }
}

extend<U> InheritedDefaultOwner<U> where U <: Bound {
    private func inherited(value: Int64): Unit { }
}

extend Int64 <: Bound { }

class PublicInheritedDefaultOwner<T> <: Child<T> {
    init() { }
}

extend<U> PublicInheritedDefaultOwner<U> where U <: Bound {
    public func inherited(value: Int64): Unit { }
}

interface ExactDefaultSurface<T> {
    func inheritedExact(value: T): Unit { }
}

open class ExactParent<T> <: ExactDefaultSurface<T> {
    init() { }
}

extend<T> ExactParent<T> {
    private func <!EXTEND_MEMBER_CANNOT_SHADOW, WEAK_VISIBILITY!>inheritedExact<!>(value: T): Unit { }
}

open class ExactChild<T> <: ExactParent<T> {
    init() { }
}

class ExactLeaf<T> <: ExactChild<T> {
    init() { }

    public func inheritedExact(value: Int64): Unit { }
}

func instantiate(): Unit {
    let collision = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateOwnerCollision<Int64><!>()
    let isolated = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>UnrelatedExtendOwners<Int64><!>()
    let inheritedPrivateExtend = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>Leaf<Int64><!>()
    let inheritedDefault = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>InheritedDefaultOwner<Int64><!>()
    let publicInheritedDefault = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PublicInheritedDefaultOwner<Int64><!>()
    let exactInheritedPrivateExtend = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ExactLeaf<Int64><!>()
}
