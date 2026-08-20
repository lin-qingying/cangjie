// RUN_PIPELINE_TILL: FRONTEND
// FILE: base.cj
package visibilitydirect

private func hiddenTopLevel(): Int64 {
    return 1
}

public open class Box {
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
    return <!UNRESOLVED_REFERENCE!>hiddenTopLevel<!>()
}

class Child <: Box {
    func hiddenMemberUse(): Int64 {
        return this.<!NOT_MEMBER_OF!>hiddenMember<!>()
    }
}
