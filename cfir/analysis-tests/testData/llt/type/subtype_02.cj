

main() {
    let t1: Option<ToString> = Option<ToString>.Some(Test1)
    let t2: Option<Test> = Option<Test>.None
    doSomething(t1)
    doSomething(<!TYPE_MISMATCH!>t2<!>)
}

func doSomething(a: Option<ToString>) {}

enum Test <: ToString {
    Test1

    public func toString() {
        "test"
    }
}
