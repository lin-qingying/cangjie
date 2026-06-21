// FILE: pkga.cj


package pkga

public class A {
    var map: Int64 = 10
}

// FILE: access_check.cj



import pkga.A

public func map(): Int64 {
    return 20
}

extend A {
    public func test(): Unit {
        var a: Int64 = map()
    }
}

main() {}
