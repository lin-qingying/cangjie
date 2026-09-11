interface First {}
interface Second {}
class Left <: First & Second {}
class Right <: First & Second {}
class One <: First {}
class Two <: First {}
class Plain {}
open class Parent <: First {}
class ChildOne <: Parent {}
class ChildTwo <: Parent {}
type ParentAlias = Parent

func standardMatch(flag: Bool) {
    <!TYPE_INCOMPATIBLE!>match (flag) { case true => 1; case false => "text" }<!>
}
func standardIf(flag: Bool) {
    <!TYPE_INCOMPATIBLE!>if (flag) { 1 } else { "text" }<!>
}
func ambiguousMatch(flag: Bool) {
    <!TYPE_INCOMPATIBLE!>match (flag) { case true => Left(); case false => Right() }<!>
}
func interfaceAndObjectMatch(flag: Bool) {
    <!TYPE_INCOMPATIBLE!>match (flag) { case true => One(); case false => Two() }<!>
}
func anyMatch(flag: Bool) {
    match (flag) { case true => Plain(); case false => 1 }
}
func tupleMatch(flag: Bool) {
    match (flag) { case true => (1, true); case false => ("text", true) }
}
func functionMatch(flag: Bool) {
    match (flag) { case true => { => 1 }; case false => { => "text" } }
}
func declaredAny(flag: Bool): Any {
    match (flag) { case true => 1; case false => "text" }
}
func declaredIfAny(flag: Bool): Any {
    if (flag) { 1 } else { "text" }
}
func declaredInterface(flag: Bool): First {
    match (flag) { case true => One(); case false => Two() }
}
func uniqueClassMatch(flag: Bool): ParentAlias {
    match (flag) { case true => ChildOne(); case false => ChildTwo() }
}
