


struct R1<T> {
    <!CLASS_UNINITIALIZED_FIELD!>p<!>ublic var i: T

    public mut func foo1() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his
        }
        return 0
    }

    public mut func foo2(x: T) {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = x}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i = <!CANNOT_CONVERT_LITERAL!>2<!>
        }
        return 0
    }

    public mut func foo3() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>t<!>his.i
        }
        return 0
    }

    public mut func foo4() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>i<!>}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>i<!>
        }
        return 0
    }

    public mut func foo5() {
        return <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
    }

    public mut func foo6() {
        var a = <!THIS_AS_EXPRESSION_IN_FUNC!>t<!>his
        return a
    }
}

main() {
    return 1
}
