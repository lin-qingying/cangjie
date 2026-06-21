


interface A {
    func add(): Unit
}

interface B {
    func add(): Unit
}

class S {}

extend S <: A {
    public func add(): Unit {}
}

extend S <: B {}
