

class A {
    public var a: Int64
    init(a_: Int64) {
        a = a_
    }
    operator func [](arg: A, value!: A) {
        this.a = arg.a
    }
}

func test01() {
    var a = A(1)
    <!TYPE_MISMATCH!>(a.<!NOT_MEMBER_OF!>x<!>, a.<!NOT_MEMBER_OF!>y<!>) = (1, 2, 8)<!>
}

func test02() {
    var class_a = A(1)
    var class_b = A(1)
    var class_c = A(1)
    <!TYPE_MISMATCH!>(<!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_a[class_b]<!>, <!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_b[class_a]<!>) = (1, 2)<!>
    <!TYPE_MISMATCH!>(class_a[class_b], class_b[class_a]) = 1<!>
    <!TYPE_MISMATCH!>(<!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_a[class_b]<!>, <!CANNOT_ASSIGN_TO_SUBSCRIPT!>class_b[class_a]<!>) = (1, 2, 3)<!>
}

main() {
}
