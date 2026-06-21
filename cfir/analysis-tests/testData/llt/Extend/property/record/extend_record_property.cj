

struct A {
    A(var a!: Int64 = 1) {}
}

extend A {
    mut prop pa: Int64 {
        get() {
            a
        }
        set(b) {
            a = b
        }
    }
}

main() {
    var a = A()
    a.pa = 2
    return a.pa - 2
}
