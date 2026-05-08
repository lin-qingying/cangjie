package visibilitymatrix.other
import visibilitymatrix.*

class ExternalChild <: Base {
    func readProtectedFromSubclass(): Int64 {
        return protectedValue()
    }

    func readInternalFromSubclass(): Int64 {
        return internalValue()
    }
}

func crossPackageProtected(base: Base): Int64 {
    return base.protectedValue()
}

func crossPackageInternal(base: Base): Int64 {
    return base.internalValue()
}
