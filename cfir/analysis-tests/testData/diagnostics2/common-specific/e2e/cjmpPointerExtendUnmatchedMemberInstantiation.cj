// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// Private common members participate in generic-instantiation checking while remaining inaccessible to call resolution.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_pointer_instantiation

common extend<T> CPointer<T> {
    private func select(value: T): Unit { }
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_pointer_instantiation

specific extend<U> CPointer<U> {
    <!NOT_MATCHED!>public specific func select(value: Int64): Unit { }<!>
}

func trigger(): Unit {
    let pointer = <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS!>CPointer<Int64><!>()
    pointer.select(1)
}
