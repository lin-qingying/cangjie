


open class C1 {
    public func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public <!REDEF_INSTANCE_ERROR, REDUNDANT_MODIFIER_FOR_TARGET!>redef<!> func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
