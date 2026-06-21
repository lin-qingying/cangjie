// FILE: testa.cj


package a

public interface My {}

public interface A<T> {
    func st(): My
}

// FILE: testb.cj


package b

import a

public open class My <: a.My {}

public open class A <: a.A<Int64> {
    public open func st(): My {
        My()
    }
}

// FILE: main.cj



package c

import b

public class A <: b.A {
    public func some() {
        let dd: b.My = st()
    }
}

main() {}
