


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
}

main() {
    let a = A()
    a.a += "str"
    if (a.a != "0str") {
        return 1
    }
    return 0
}
