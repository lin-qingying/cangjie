

// DIAGNOSTICS: -UNUSED_VARIABLE

class Aoo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> where T <: Bool & Object {
    public var a: T
    public init(x: T) {
        a = x
    }
}

main() {
    var a = Aoo<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>Bool<!>>(false)
    if (!a.a) {
        return 1
    }
    return 0
}
