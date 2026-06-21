


interface I {
    open func f() {}
    mut open prop b: Int64 {
        get() {
            print("get")
            0
        }
        set(value) {
            print("set")
        }
    }
    func g() {}
}

main() {
    return 0
}
