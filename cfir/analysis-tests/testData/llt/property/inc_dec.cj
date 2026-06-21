


struct R {
    var a: Int64 = 0
    mut prop p: Int64 {
        get() {
            a
        }
        set(v) {
            a = v
        }
    }
    mut func inc() {
        p++
    }
    mut func dec() {
        p--
    }
}

main() {
    var r = R()
    r.p++
    if (r.a != 1) {
        return 1
    }
    r.p--
    if (r.a != 0) {
        return 2
    }
    r.inc()
    if (r.a != 1) {
        return 3
    }
    r.dec()
    if (r.a != 0) {
        return 4
    }
    return 0
}
