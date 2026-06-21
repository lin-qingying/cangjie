


class A {
    var b0: Int64 = 1
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
    a?.b--
    a?.b ?? 1
}
