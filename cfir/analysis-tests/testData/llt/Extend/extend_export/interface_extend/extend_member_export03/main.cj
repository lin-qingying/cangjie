// FILE: testa.cj


package a

public class A {}

// FILE: testb.cj


package b

import a.*

public interface I {
    public prop f1: Int64 {
        get() {
            1
        }
    }
    public prop f4: Int64
}

public interface I2 <: I {
    public prop f3: Int64
}

extend A <: I2 {
    public prop f2: Int64 {
        get() {
            2
        }
    }
    public prop f3: Int64 {
        get() {
            3
        }
    }
    public prop f4: Int64 {
        get() {
            4
        }
    }
}

// FILE: main.cj


import a.*
import b.*

main() {
    A().f1
    A().<!NOT_MEMBER_OF!>f2<!> // error
    A().f3
    A().f4
}
