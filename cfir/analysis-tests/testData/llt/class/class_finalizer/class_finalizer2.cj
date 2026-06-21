


class A {
    public var x = 1
    ~init() {
        f(<!INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER!>this<!>)
        let a = {=> this.x}
        {=> x}
        <!INSTANCE_FUNC_CANNOT_BE_USED_IN_FINALIZER!>this<!>
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
