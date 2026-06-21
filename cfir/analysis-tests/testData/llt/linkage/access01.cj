


package a.b.c

import <!UNRESOLVED_IMPORT!>a<!>.b.*

main() {
    println(AAAA().f1())
    println(BBBB().f1())
    println(CCCC().f1())
    println(DDDD().f1())
    println(DDDD().f2())
    println(DDDD().f3())
    println(EEEE().f1())
    println(EEEE().f2())
    println(EEEE().f3())
    println(FFFF().f1())
    println(FFFF().f2())
    println(FFFF().f3())
}
