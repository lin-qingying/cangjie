

package a
interface I {
    func foo() {
        println(0)
    }
}

protected class C <: I {
}
public class D <: I {
}

public class E {}
extend E <: I {}
