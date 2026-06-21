// FILE: pkg/pkg.cj


package pkg

public interface I {}

public class C {}

extend C <: I {}

// FILE: main.cj



internal import pkg.*

interface II <: I {}

extend C <: II {}

main() {
    0
}
