// FILE: pkg.cj


package pkg

internal import pkg1.*

public interface I<T> {}

extend<T2> A<T2> <: I<T2> {}

extend<T2> B<T2> <: I<Int64> {}

// FILE: pkg1.cj


package pkg1

public class A<T1> {}

public class B<T1> {}

// FILE: main.cj



internal import pkg1.*

internal import pkg.*

interface I2<T> <: I<T> {}

extend<T3> A<T3> <: I2<T3> {}

extend<T3> <!TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE!>B<T3><!> <: I2<T3> {}

main() {}
