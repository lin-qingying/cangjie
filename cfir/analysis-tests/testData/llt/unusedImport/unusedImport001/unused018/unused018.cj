// FILE: a.cj


package a

public class A {}

// FILE: b.cj


package b

import a.A

public interface I {
    func testFunc(): Unit
}

extend A <: I {
    public func testFunc(): Unit {}
}

// FILE: c.cj


package c

import a.A

public enum Type {
    | TypeA(A)
}

// FILE: unused018.cj



package d

import c.Type

public func test(typeNum: Type): Unit {
    match (typeNum) {
        case TypeA(a) => return
    }
}

main() {
}
