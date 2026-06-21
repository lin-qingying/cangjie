// FILE: base.cj
package pkg.base

public open class Base {
    let value: Int64 = 0
}



// FILE: derived.cj
package pkg.derived

import pkg.base.Base

open class Derived <: Base {
    let value: Int64 = 1
}



// FILE: test.cj
package pkg.derived.sub

import pkg.derived.Derived

public class SubDerived <: Derived {
    public func print(): Unit {
        println(value)
    }
}


