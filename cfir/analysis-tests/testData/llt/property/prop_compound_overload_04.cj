


// Test for side effect of compound calculation with property operator overloading
var count = 0

class A {
    var a_ = "0"
    mut prop a: String {
        get() {
            a_
        }
        set(v) {
            a_ = v
        }
    }
    init() {
        count++
    }
}

main() {
    A().a += "str"
    if (count != 1) {
        return 1
    }
    return 0
}
