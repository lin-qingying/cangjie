

open class A {}

class B <: A {}

interface I {
    static func f1(): A
    static func f2(): Unit {
        f1()
    }
}

interface I1 <: I {
    static func f1(): B {
        println(1)
        B()
    }
}

main() {
    I1.f2()
}
