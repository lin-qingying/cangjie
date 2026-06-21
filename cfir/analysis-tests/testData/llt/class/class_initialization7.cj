


open class A {
    let x: A
    public A(x: A) {
        this.x = x
    }
}

class B <: A {
    public B() {
        super(this.<!ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER!>x<!>)
    }
}

main() {
    let b = B()
    let a = b.x
    let aa = a.x
    return 0
}
