struct S {
    public func structLeak(): Int64 { return 1 }
}
enum E {
    A
    public func enumLeak(): Int64 { return 2 }
}
type SAlias = S
open class G<T> {
    public func genericLeak(): Int64 { return 3 }
}
func ownMemberBeforeDeclaration(): Int64 {
    return FromArity().ownMember()
}
class NoParent {
    public func check(): Int64 { return <!UNRESOLVED_REFERENCE!>structLeak<!>() }
}
class FromStruct <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>S<!> {
    public func check(): Int64 { return <!UNRESOLVED_REFERENCE!>structLeak<!>() }
}
class FromEnum <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>E<!> {
    public func check(): Int64 { return <!UNRESOLVED_REFERENCE!>enumLeak<!>() }
}
class FromAlias <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>SAlias<!> {
    public func check(): Int64 { return <!UNRESOLVED_REFERENCE!>structLeak<!>() }
}
class FromArity <: <!GENERIC_ARGUMENT_NO_MATCH!>G<Int64, String><!> {
    public func check(): Int64 { return <!UNRESOLVED_REFERENCE!>genericLeak<!>() }
    public func ownMember(): Int64 { return 4 }
}
func ownMemberAfterDeclaration(): Int64 {
    return FromArity().ownMember()
}
func directWrongArityReceiverCascade(): Int64 {
    return <!GENERIC_ARGUMENT_NO_MATCH!>G<Int64, String>()<!>.genericLeak()
}
main(): Int64 {
    return FromArity().<!NOT_MEMBER_OF!>genericLeak<!>()
}
