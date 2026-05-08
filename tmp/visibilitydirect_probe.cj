// FILE: base.cj
package visibilitydirect
private func hiddenTopLevel(): Int64 { return 1 }
open class Box { private func hiddenMember(): Int64 { return 2 } }
// FILE: use.cj
package visibilitydirect.other
import visibilitydirect.*
func hiddenTopLevelUse(): Int64 { return hiddenTopLevel() }
class Child <: Box { func hiddenMemberUse(): Int64 { return this.hiddenMember() } }
