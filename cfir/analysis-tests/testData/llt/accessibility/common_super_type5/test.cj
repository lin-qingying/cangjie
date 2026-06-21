// FILE: pkg.cj
package pkg

import pkg.base.Base

public open class Derived <: Base {
    let value: Int64 = 1
    prop b: Int64{
        get() {
            value
        }
    }
}



// FILE: base.cj
package pkg.base

public open class Base {
    let value: Int64 = 0
    prop b: Int64{
        get() {
            value
        }
    }
}



// FILE: test.cj
package pkg.base.derived

import pkg.Derived

public class SubDerived <: Derived {
    public func print(): Unit {
        println(b)
    }
}


