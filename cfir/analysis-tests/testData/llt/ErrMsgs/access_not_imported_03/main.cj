// FILE: pkg1.cj


package pkg1

public var x = 1
public var z = 2

// FILE: pkg2.cj


package pkg2

public import pkg1.z

public var y = 1

// FILE: main.cj



internal import pkg2.y

// Should report "not member of package". NOT "pkg2.x is not imported"
main() {
    println(pkg2.<!UNRESOLVED_REFERENCE!>x<!>)
}
