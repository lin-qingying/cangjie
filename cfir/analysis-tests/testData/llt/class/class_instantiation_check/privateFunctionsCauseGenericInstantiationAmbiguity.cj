// LANGUAGE_VERSION: 1.1.0

class PrivateGeneric<T> {
    private func select(value: T): Unit { }
    private func select(value: Int64): Unit { }
}

func instantiate(): <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateGeneric<Int64><!> {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>PrivateGeneric<Int64><!>()
}

interface DefaultFunctionSurface {
    func select(value: Int64): Unit { }
}

class ExtendedGeneric<T> {
    init() { }
}

extend<T> ExtendedGeneric<T> <: DefaultFunctionSurface {
    func select(value: T): Unit { }
}

extend<T> ExtendedGeneric<T> {
    private func <!WEAK_VISIBILITY!>select<!>(value: Int64): Unit { }
}

func instantiateOwner(c: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>ExtendedGeneric<Int64><!>): Unit { }
