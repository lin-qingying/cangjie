// FILE: testa.cj


package pkg1

public interface MyInterface {
    static func f<T>(a: T): T {
        a
    }
    func g(): Int64 {
        f<Int64>(1)
    }
}

extend Int64 <: MyInterface {}

public class A {}

extend A <: MyInterface {}

// FILE: main.cj


import pkg1.*

main() {
    Int64.f<Int16>(1)
    1.g()
    A.f<Int16>(1)
    A().g()

    return 0
}
