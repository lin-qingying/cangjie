



class X {}

interface Y {}

enum R<T> where T <: X & Y {
    e
}

var a = R<X>.e
var b = R<Y>.e

main(): Int64 {
    return 0
}

