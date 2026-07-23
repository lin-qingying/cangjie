class FinalGeneric<T> {}

class WrongArityFinal <: <!GENERIC_ARGUMENT_NO_MATCH!><!NON_INHERITABLE_SUPER_CLASS!>FinalGeneric<!><Int64, String><!> {}

<!INHERITANCE_CYCLE!>class ArityA<T><!> <: <!GENERIC_ARGUMENT_NO_MATCH!><!NON_INHERITABLE_SUPER_CLASS!>ArityB<!><Int64, String><!> {}
class ArityB<T> <: <!GENERIC_ARGUMENT_NO_MATCH!><!NON_INHERITABLE_SUPER_CLASS!>ArityA<!><Int64, String><!> {}

main(): Int64 { return 0 }
