// FILE: a.cj
package a 

public class MyClass {}




// FILE: b.cj
package b

public import a.MyClass




// FILE: unused015.cj
package c
import <!UNUSED_IMPORT!>b.MyClass<!>

main() {
}


