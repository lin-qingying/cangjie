open class Base {}
class Sub <: Base {}

class A {
    public func g(a: Base): Int64 { 1 }
    public func g(a: Sub): Int64 { 2 }
}

class C {
    public func h(a: (Base) -> Int64) {}
    public func h2(a: (Sub) -> Int64) {}
}

main() {
    let a = A()
    let c = C()
    c.h(a.g)
    c.h2(a.g)
}
