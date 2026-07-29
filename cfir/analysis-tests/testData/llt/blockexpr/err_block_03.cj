

class A {}

var a: Int64 = 1

main() {
    if (a <= if (false) {
        let b: A = if (false) {
            <!TYPE_MISMATCH!>let c = 1<!>
        } else {
            A()
        }
        1i64
    } else {
        1
    }) {
        return 1
    }
    return 0
}
