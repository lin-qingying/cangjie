

class A {
    public let b: Int32 = 0
    public let c = {
        => <!USED_BEFORE_INITIALIZATION!>foo<!>()
    }
    public let d = {
        => let e = {
            => let f = {
                => let g = {
                    => <!USED_BEFORE_INITIALIZATION!>foo<!>()
                }
            }
        }
    }
    public init(str: String) {
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = foo()
    }

    init() {
        func ff() {
            func fff() {
                func ffff() {
                    foo()
                }
            }
        }
    }

    init(n: Int64) {
        var a = {
            => func ff() {
                var b = {
                    => func fff() {
                        this.foo()
                    }
                }
            }
        }
    }
    public func foo(): Int32 {
        0
    }
}

main(): Int64 {
    0
}
