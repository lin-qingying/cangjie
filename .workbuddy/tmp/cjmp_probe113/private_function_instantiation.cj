package privateinstantiationprobe

class PrivateGeneric<T> {
    private func select(value: T): Unit { }
    private func select(value: Int64): Unit { }
}

func instantiate(): PrivateGeneric<Int64> {
    PrivateGeneric<Int64>()
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
    private func select(value: Int64): Unit { }
}

func instantiateOwner(c: ExtendedGeneric<Int64>): Unit { }
