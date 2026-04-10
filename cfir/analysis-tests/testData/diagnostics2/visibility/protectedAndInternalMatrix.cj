// RUN_PIPELINE_TILL: FRONTEND
// FILE: base_same_package.cj
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

// FILE: other_package.cj
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

// FILE: unrelated_package.cj
package outsider
import visibilitymatrix.*

func unrelatedPackageInternal(base: Base): Int64 {
    return base.<!INVISIBLE_MEMBER!>internalValue<!>()
}

func unrelatedPackageProtected(base: Base): Int64 {
    return base.<!INVISIBLE_MEMBER!>protectedValue<!>()
}
