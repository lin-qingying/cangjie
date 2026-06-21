// FILE: pkg1/pkg1.cj


package test.pkg1

import test.pkg0.*

public const func bar(): Unit {
    foo<UInt32>()
    ()
}

// FILE: main.cj



package test

import test.pkg1.*

main() {
    bar()
}
