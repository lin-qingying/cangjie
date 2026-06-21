


// Test for sema check of foreign function assignment.

foreign {
    func cb(a: Int64): Unit

    func ff(a: CFunc<(Int64) -> Unit>): Unit
}

main() {
    let f: CFunc<(Int64) -> Unit> = cb
    unsafe { ff(cb) }
    0
}
