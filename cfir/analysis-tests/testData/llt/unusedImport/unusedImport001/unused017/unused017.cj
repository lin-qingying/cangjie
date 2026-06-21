// FILE: a.cj
package a 

public interface I {
    func testFunc() : Unit
}

public interface X {
}

extend<T> Array<T> <: I where T<: X {
    public func testFunc() : Unit{}
}



// FILE: b.cj
package b

import a.X

public class A <: X {}



// FILE: unused017.cj
package c
<!UNUSED_IMPORT!>import a.X<!>
<!UNUSED_IMPORT!>import a.I<!>
import b.A

main() {
   var a = Array<A>()
   return 0
}


