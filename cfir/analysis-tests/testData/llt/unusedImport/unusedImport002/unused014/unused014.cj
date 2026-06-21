// FILE: a.cj
package a 

public import std.collection.*

public class MyClass {}



// FILE: b.cj
package b

public import a.*




// FILE: unused014.cj
package c
import b.*

main() {
    var a : ArrayList<Int64>
}


