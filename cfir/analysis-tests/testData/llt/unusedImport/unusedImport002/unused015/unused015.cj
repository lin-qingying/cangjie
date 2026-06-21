// FILE: b.cj
package b

public import std.convert.Parsable




// FILE: unused015.cj
package c
import b.*

main() {
    println(UInt32.parse("123"))
    return 0
}


