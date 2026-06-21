

// functions within different extensions of the same type can call each other.
class A {}

extend A {
    func goo() {
        foo()
    }
}

extend A {
    public func foo() {}
}

main() {
    A().goo()
    return 0
}
