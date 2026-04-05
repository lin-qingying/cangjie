// RUN_PIPELINE_TILL: FRONTEND
// FILE: hidden.cj
package visibilitycase

private func hiddenTopLevel(): Int64 {
    return 1
}

open class Base {
    private func hiddenMember(): Int64 {
        return 2
    }

    func visibleInside(): Int64 {
        return hiddenMember()
    }
}

// FILE: use.cj
package visibilitycase

func useHiddenTopLevel(): Int64 {
    return <!INVISIBLE_REFERENCE!>hiddenTopLevel<!>()
}

class Child <: Base {
    func useHiddenMember(): Int64 {
        return this.<!INVISIBLE_MEMBER!>hiddenMember<!>()
    }
}
