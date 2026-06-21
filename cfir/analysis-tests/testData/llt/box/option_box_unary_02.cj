

class A {
    operator func -() {
        1
    }
}

main() {
    var count = 0
    let a: ?ToString = -(Some(A()) ?? A())
    if (let Some(v) <- a) {
        if (v.toString() != "1") {
            return 1
        }
        count++
    }
    let b: ?ToString = -(Some(11) ?? 0)
    if (let Some(v) <- b) {
        if (v.toString() != "-11") {
            return 2
        }
        count++
    }
    let c: ?ToString = !1
    if (let Some(v) <- c) {
        if (v.toString() != "-2") {
            return 3
        }
        count++
    }
    if (count != 3) {
        return 4
    }
    return 0
}
