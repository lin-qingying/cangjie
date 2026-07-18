


open class A<X, Y> {}

open class B<X, Y> <: <!AMBIGUOUS_USE!><!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!><X, Y><!> {}

open class C<X, Y> <: <!AMBIGUOUS_USE!><!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!><X, Y><!> {}

func foo<Xot>() {}

open class <!CLASSIFIER_REDECLARATION!>A<!><X, Y> {}

open class <!CLASSIFIER_REDECLARATION!>B<!><X, Y> <: <!AMBIGUOUS_USE!><!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!><X, Y><!> {}

open class <!CLASSIFIER_REDECLARATION!>C<!><X, Y> <: <!AMBIGUOUS_USE!><!CLASS_INHERIT_NON_CLASS_NOR_INTERFACE!>A<!><X, <!UNDECLARED_TYPE_NAME!>Z<!>, Y><!> {}

func foo<X, Y>(a: <!AMBIGUOUS_USE!>B<Y, X><!>, b: <!AMBIGUOUS_USE!>C<Y, X><!>): Int64 {
    0
}

main() {
    <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>foo<!>(B<Int32, Int64>(), C<Int32, Int64>())
}
