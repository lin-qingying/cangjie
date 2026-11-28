


struct R1<T> {
    public var <!CLASS_UNINITIALIZED_FIELD!>i<!>: T

    public mut func foo1() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>
        }
        return 0
    }

    public mut func foo2(x: T) {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>.i = x}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>.i = <!CANNOT_CONVERT_LITERAL!>2<!>
        }
        return 0
    }

    public mut func foo3() {
        let f = {=> <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>.i}

        func goo() {
            <!CAPTURE_THIS_OR_INSTANCE_FIELD_IN_FUNC!>this<!>.i
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
        return <!THIS_AS_EXPRESSION_IN_FUNC!>this<!>
    }

    public mut func foo6() {
        var a = <!THIS_AS_EXPRESSION_IN_FUNC!>this<!>
        return a
    }
}

main() {
    return 1
}
