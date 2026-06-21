

package test

public interface A {
    func a(): Int
}

public interface MarkerA <: A {
}

class Foo {}

extend Foo <: A {
    public func a() { 9 }
}

extend Foo <: MarkerA {
}

main() {
    println(Foo().a())
    0
}
