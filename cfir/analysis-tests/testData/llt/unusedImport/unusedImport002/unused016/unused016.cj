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



// FILE: unused016.cj
package c
import a.X
import a.I
import b.A

main() {
   var a = Array<A>()
   a.testFunc()
   return 0
}


