// FILE: pkg1.cj


package pkg1

public let a: Array<(Int16, Int16, Int16)> = [(1, 2, 3)]

// FILE: pkg2.cj


package pkg2

public let b: Array<(Int16, Int16, Int16)> = [(1, 2, 3)]

// FILE: main.cj


internal import pkg1.*
internal import pkg2.*

// Test for import instantation of same array type from different & irrelevant packages
main(): Int64 {
    let b = a.toArray()
    0
}
