// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 two-stage probe: a peer common extend's inherited default conflicts with an unmatched specific CPointer member.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_pointer_peer_default

public common interface Default<T> {
    public common func select(value: T): Unit { }
}

common extend<T> CPointer<T> { }

common extend<T> CPointer<T> <: Default<T> { }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_pointer_peer_default

public specific interface Default<U> { }

specific extend<U> CPointer<U> {
    <!NOT_MATCHED!>public specific func select(value: Int64): Unit { }<!>
}

func trigger(): Unit {
    let pointer = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>CPointer<Int64><!>()
}
