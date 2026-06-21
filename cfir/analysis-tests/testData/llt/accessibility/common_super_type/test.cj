// FILE: pa/a.cj


package pa

public interface I0 {}

// FILE: pb/b.cj


package pb

private import pa.*

interface I1 <: I0 {}

interface I2 <: I0 {}

main() {
    let i2: I0 = <!TYPE_MISMATCH!>i<!>
}

// FILE: test.cj

