// FILE: testa.cj


package a

public class A {}

// FILE: testb.cj


package b

import a.*

public interface I {
    public func f1(): Unit {
        println("f1")
    }
    public func f3(): Unit
}

extend A <: I {
    public func f2() {
        println("f2")
    }
    public func f3() {
        println("f3")
    }
}

// FILE: main.cj



import a.*
import b.*

main() {
    A().f1()
    A().<!NOT_MEMBER_OF!>f2<!>() // error
    A().f3()
}
