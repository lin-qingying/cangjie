// FILE: pkg/pkg.cj


package pkg

public class A {}

public class A1 {}

public interface B {}

// FILE: main.cj



internal import pkg.*

type C = A

type D = B

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>C<!> <: D {}

interface E <: D {}

extend <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>A1<!> <: E {}

main() {
    return 0
}
