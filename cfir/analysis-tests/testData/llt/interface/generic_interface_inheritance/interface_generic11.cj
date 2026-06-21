


open class int16 {}

open class int32 <: int16 {}

open class int64 <: int32 {}

open class float16 {}

open class float32 <: float16 {}

open class float64 <: float32 {}

open class bool {}

interface I<T> {
    func foo(a: T): T
    static func fs(a: T): T
}

interface II<T> <: I<T> & I<int32> & I<float64> {
    func foo(a: int32): int32
    func foo(a: float64): float64

    static func fs(a: int32): int32
    static func fs(a: float64): float64
}

<!SUPER_TYPES_DUPLICATE!>interface III2<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<int32><!> {
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

<!SUPER_TYPES_DUPLICATE!>interface III3<!> <: <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<int32><!> {
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III4<T> <: II<T> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<float64><!> {
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

interface III5<T> <: II<T> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<float64><!> {
    func foo(a: float64): float64
    static func fs(a: float64): float64
}

<!SUPER_TYPES_DUPLICATE!>interface III6<!> <: II<int64> & II<int64> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
}

interface III7<T> <: II<T> & II<int64> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<int32><!> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

interface III18<T> <: II<T> & II<int64> & <!GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS, SUPER_TYPES_DUPLICATE!>II<int32><!> {
    func foo(a: int64): int64
    static func fs(a: int64): int64
    func foo(a: int32): int32
    static func fs(a: int32): int32
}

main() {
    return 0
}
