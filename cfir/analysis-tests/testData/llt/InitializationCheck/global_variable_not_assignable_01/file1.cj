// FILE: file2.cj


main() {
    let a = A()
    a.foo()
    return 0
}

const res1 = 0
let res2 = 0

// FILE: file1.cj



class A {
    public func foo() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>res1<!> += 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>res2<!> += 1
        return
    }
}
