struct S {}
enum E { A }
type SAlias = S
func F(): Unit {}
open class G<T> {}

class FromStruct <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>S<!> {}
class FromEnum <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>E<!> {}
class FromAliasToStruct <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>SAlias<!> {}
open class FromGeneric<T> <: <!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>T<!> {}
class FromNotAType <: <!NOT_A_TYPE!>F<!> {}
class FromWrongArity <: <!GENERIC_ARGUMENT_NO_MATCH!>G<Int64, Int64><!> {}
class FromUndeclared <: <!UNDECLARED_TYPE_NAME!>Missing<!> {}

main(): Int64 { return 0 }
