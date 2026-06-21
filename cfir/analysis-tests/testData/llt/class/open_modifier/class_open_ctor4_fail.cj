


abstract class A {
    var b: B
    init() {
        b = B()
        b.bind(<!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his)
    }
}

class B {
    var a: Option<A> = Option<A>.None
    public func bind(a: A) {
        this.a = a
    }
}

main(): Int64 {
    0
}
