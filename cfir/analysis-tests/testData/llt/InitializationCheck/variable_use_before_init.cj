

main(): Int64 {
    return 0
}

class Aoo {
    public var a: Int32 = 1
    public let b: Int32 = 2
    public var c = if (true) {
        func foo1(x!: Int32 = <!ILLEGAL_USAGE_OF_MEMBER!>a<!>) {
            return x
        }
        func foo2() {
            var x = <!ILLEGAL_USAGE_OF_MEMBER!>a<!>;
            return x
        }
        func foo3() {
            return <!ILLEGAL_USAGE_OF_MEMBER!>a<!>
        }
        func foo4() {
            <!ILLEGAL_USAGE_OF_MEMBER!>a<!>;
            return 1
        }
        func foo5() {
            <!ILLEGAL_USAGE_OF_MEMBER!>a<!>
        }

        func goo1() {
            var a = foo1
            return a
        }
        func goo2() {
            var a = foo2
            return a
        }
        func goo3() {
            var a = foo3
            return a
        }
        func goo4() {
            var a = foo4
            return a
        }
        func goo5() {
            var a = foo5
            return a
        }
    }
}
