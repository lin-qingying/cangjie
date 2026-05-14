package outsider
import visibilitymatrix.*

func unrelatedPackageInternal(base: Base): Int64 {
    return base.internalValue()
}

func unrelatedPackageProtected(base: Base): Int64 {
    return base.protectedValue()
}
