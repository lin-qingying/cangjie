


class A {
    var b0: Int64 = 0
    mut prop b: Int64 {
        get() {
            b0
        }
        set(value) {
            b0 = value
        }
    }
}

main() {
    var a = Some(A())
    a?.b = 1
    match (a?.b) {
        case Some(t) => t - 1
        case None => 1
    }
}
