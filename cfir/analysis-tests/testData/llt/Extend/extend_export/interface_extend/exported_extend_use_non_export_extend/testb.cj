// FILE: testa.cj


package a.b

public class A {}

// FILE: testb.cj


package a.c

import a.b.*

interface I {
    func foo(): Unit
}

<!EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND!>extend A <: I {}<!>

extend A {
    public func foo(): Unit {}
}

main() {}
