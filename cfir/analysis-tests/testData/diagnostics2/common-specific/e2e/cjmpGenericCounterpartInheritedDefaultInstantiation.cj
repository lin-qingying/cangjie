// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: the common inherited default remains in the merged specific member set and conflicts after C<Int64>.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common interface I<T> {
    public common func f(value: T): Unit { }
}

public common class C<T> <: I<T> {
    public common init()
}

public common class Overrides<T> <: I<T> {
    public common init()
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

public specific interface I<U> { }

public specific class C<U> <: I<U> {
    public specific init() { }
    <!NOT_MATCHED!>public specific func f(value: Int64): Unit { }<!>
}

public specific class Overrides<U> <: I<U> {
    public specific init() { }
    <!NOT_MATCHED!>public specific func f(value: U): Unit { }<!>
}

func trigger(): <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int64><!> {
    <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>C<Int64><!>()
}

func triggerOverride(): Overrides<Int64> {
    Overrides<Int64>()
}
