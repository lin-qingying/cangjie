// RUN_PIPELINE_TILL: FRONTEND
// FILE: base.cj
package visibilitydirect

private func hiddenTopLevel(): Int64 {
    return 1
}

open class Box {
    private func hiddenMember(): Int64 {
        return 2
    }

    func okInside(): Int64 {
        return hiddenMember()
    }
}

// FILE: use.cj
package visibilitydirect.other
import visibilitydirect.*

func hiddenTopLevelUse(): Int64 {
    return <!INVISIBLE_REFERENCE!>hiddenTopLevel<!>()
}

class Child <: Box {
    func hiddenMemberUse(): Int64 {
        return this.<!INVISIBLE_MEMBER!>hiddenMember<!>()
    }
}
