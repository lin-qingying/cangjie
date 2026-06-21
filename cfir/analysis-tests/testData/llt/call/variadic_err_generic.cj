

extend<T> Array<T> {
    public func reduce<Tnew>(cb: ((Array<T>) -> T), i: Tnew) {
        var y: Tnew = i
        for (i in 0..this.size) {
            y = <!TYPE_MISMATCH!>cb(this)<!>
        }
        y
    }
}

main() {}
