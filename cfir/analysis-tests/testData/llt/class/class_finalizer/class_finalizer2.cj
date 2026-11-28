


class A {
    public var x = 1
    ~init() {
        f(<!THIS_AS_EXPRESSION_IN_FUNC!>this<!>)
        let a = {=> this.x}
        {=> x}
        <!THIS_AS_EXPRESSION_IN_FUNC!>this<!>
        func g() {
            x
        }
    }
}

func f(a: A) {
    return a
}

main() {
    0
}
