


interface I {
    func coo() {}
}

open class A <: I {
    protected func foo() {}
}

extend A {
    func goo() {}
}

open class B <: A {
    public init() {
        { // lambda
            => <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        }
        let v = <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        test(<!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his)
    }
}

func test(v: B) {
    1
}

main(): Int64 {
    0
}
