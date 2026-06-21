// FILE: pkg/pkg.cj


package pkg

public interface I<T> {}

public class C {}

extend C <: I<Int64> {}

// FILE: main.cj



internal import pkg.*

interface II <: I<Int8> {}

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>C<!> <: II {}

interface III <: I<Int64> {}

extend C <: III {}

main() {
    0
}
