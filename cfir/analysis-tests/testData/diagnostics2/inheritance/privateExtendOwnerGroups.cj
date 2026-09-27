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

func instantiate(): Unit {
    let collision = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateOwnerCollision<Int64><!>()
    let isolated = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>UnrelatedExtendOwners<Int64><!>()
}
