// FILE: pkg/pkg.cj


package pkg

public interface I<T> {}

public interface II<T> <: I<T> & I<Int32> & I<Float64> {}

// FILE: case.cj



internal import pkg.*

interface III1<T> <: II<T> {}

<!SUPER_TYPES_DUPLICATE!>interface III2<!> <: <!SUPER_TYPES_DUPLICATE!>II<Int32><!> {}

interface III3 <: II<Int64> {}

<!SUPER_TYPES_DUPLICATE!>interface III4<!> <: <!SUPER_TYPES_DUPLICATE!>II<Float64><!> {}

<!SUPER_TYPES_DUPLICATE!>interface III5<T><!> <: II<T> & <!SUPER_TYPES_DUPLICATE!>II<T><!> {}

interface III6<T> <: II<T> & <!SUPER_TYPES_DUPLICATE!>II<Int32><!> {}

interface III7<T> <: II<T> & II<Int64> {}

interface III8<T> <: II<T> & <!SUPER_TYPES_DUPLICATE!>II<Float64><!> {}

interface III9 <: II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Int32><!> {}

<!SUPER_TYPES_DUPLICATE!>interface III10<!> <: II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Int64><!> {}

interface III11 <: II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Float64><!> {}

interface III12<T> <: II<T> & II<Int64> & I<T> {}

interface III13<T> <: II<T> & II<Int64> & I<Int32> {}

interface III14<T> <: II<T> & II<Int64> & I<Int64> {}

interface III15<T> <: II<T> & II<Int64> & I<Float64> {}

<!SUPER_TYPES_DUPLICATE!>interface III16<T><!> <: II<T> & II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<T><!> {}

interface III17<T> <: II<T> & II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Int32><!> {}

<!SUPER_TYPES_DUPLICATE!>interface III18<T><!> <: II<T> & II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Int64><!> {}

interface III19<T> <: II<T> & II<Int64> & <!SUPER_TYPES_DUPLICATE!>II<Float64><!> {}

main() {
    return 0
}
