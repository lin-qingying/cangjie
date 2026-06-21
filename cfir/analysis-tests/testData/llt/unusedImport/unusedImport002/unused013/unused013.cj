// FILE: a.cj
package a 

public class MyClass {}




// FILE: b.cj
package b

public import a.MyClass




// FILE: unused013.cj
package c
import b.MyClass

main() {
    var myClassImpl = MyClass()
}


