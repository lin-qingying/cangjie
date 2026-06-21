// FILE: pkg.cj


package pkg

sealed interface I<T> {}

// FILE: fail.cj



internal import pkg.I

extend<T> <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>Array<T><!> <: I<T> {}

main() {}
