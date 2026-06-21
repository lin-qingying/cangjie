

class A {
    operator func -() {
        1
    }
}

main() {
    var count = 0
    let a: ?Int64 = -(Some(A()) ?? A())
    if (let Some(1) <- a) {
        count++
    }
    let b: ?Int64 = -(Some(11) ?? 0)
    if (let Some(-11) <- b) {
        count++
    }
    if (count != 2) {
        return 1
    }
    return 0
}
