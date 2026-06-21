// FILE: pkg1.cj


package pkg1

public var x = 1

// FILE: pkg2.cj


package pkg2

public import pkg1.*

public var y = 1

// FILE: main.cj



internal import pkg2.y

main() {
    println(pkg2.<!UNRESOLVED_REFERENCE!>x<!>)
}
