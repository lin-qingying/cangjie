


interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<Int32> & I<Float64> {
    func foo(a: Int32): Int32
    func foo(a: Float64): Float64

    static func fs(a: Int32): Int32
    static func fs(a: Float64): Float64
}

<!SUPER_TYPES_DUPLICATE!>interface III2<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Int32><!> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

<!SUPER_TYPES_DUPLICATE!>interface III3<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Int32><!> {
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III4<T> <: II<T> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Float64><!> {
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

interface III5<T> <: II<T> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Float64><!> {
    func foo(a: Float64): Float64
    static func fs(a: Float64): Float64
}

<!SUPER_TYPES_DUPLICATE!>interface III6<!> <: II<Int64> & II<Int64> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
}

interface III7<T> <: II<T> & II<Int64> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Int32><!> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

interface III18<T> <: II<T> & II<Int64> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<Int32><!> {
    func foo(a: Int64): Int64
    static func fs(a: Int64): Int64
    func foo(a: Int32): Int32
    static func fs(a: Int32): Int32
}

main() {
    return 0
}
