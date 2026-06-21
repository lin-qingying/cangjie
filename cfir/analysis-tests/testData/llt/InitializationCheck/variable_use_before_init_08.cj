

class A {
    static var <!TYPE_UNINITIALIZED_STATIC_FIELD!>v<!>: Int64
    static init() {
        for (i in 1..3) {
            v = i
            if (i == 2) {
                break
            }
        }
    }
}

main() {
    if (A.v == 2) {
        return 0
    }
    return 1
}
