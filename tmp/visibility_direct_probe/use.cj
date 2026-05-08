package visibilitydirect.other
import visibilitydirect.*

func hiddenTopLevelUse(): Int64 {
    return hiddenTopLevel()
}

class Child <: Box {
    func hiddenMemberUse(): Int64 {
        return this.hiddenMember()
    }
}
