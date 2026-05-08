package visibilitymatrix

open class Base {
    protected func protectedValue(): Int64 {
        return 1
    }

    internal func internalValue(): Int64 {
        return 2
    }
}

class Child <: Base {
    func readProtectedInside(): Int64 {
        return protectedValue()
    }

    func readInternalInside(): Int64 {
        return internalValue()
    }
}

func samePackageInternal(base: Base): Int64 {
    return base.internalValue()
}
