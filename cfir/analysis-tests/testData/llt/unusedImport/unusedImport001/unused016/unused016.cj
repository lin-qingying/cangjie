// FILE: a.cj
package a 

public import std.collection.*

public class MyClass {}



// FILE: b.cj
package b

public import a.*




// FILE: unused016.cj
package c
import <!UNUSED_IMPORT!>b.*<!>

main() {
}


