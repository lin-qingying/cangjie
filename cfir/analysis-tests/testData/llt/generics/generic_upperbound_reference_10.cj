

// DIAGNOSTICS: -UNUSED_VARIABLE

class A {
    func foo<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>T<!>, V>(a: T, b: V): Unit where T <: V, V <: T {}
}

main() {}
