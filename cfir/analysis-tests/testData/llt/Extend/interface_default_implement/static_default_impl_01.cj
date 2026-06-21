

interface InterfaceA {
    func foo(): Unit
    static func bar(): Unit {
        println("bar")
    }
}

interface InterfaceB <: InterfaceA {}

class ClassA {}

extend ClassA <: InterfaceA {
    public func foo(): Unit {
        println("foo")
    }
}

extend ClassA <: InterfaceB {}

main() {
    ClassA().foo()
    ClassA.bar()
}
