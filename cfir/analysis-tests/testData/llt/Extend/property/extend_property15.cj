

main(): Int64 {
    B<Int32>().<!NOT_MEMBER_OF!>a<!>
    var a = [1, 2]
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.size = 1<!>
    return 0
}

class A {}

extend A {
    public prop <!EXTEND_MEMBER_CANNOT_SHADOW!>a<!>: Int64 {
        get() {
            return 1
        }
    }
}

extend A {
    public prop <!EXTEND_MEMBER_CANNOT_SHADOW!>a<!>: Int64 {
        get() {
            return 1
        }
    }
}

class B<T> {}

extend B<Int32> {}

extend B<Int64> {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}
