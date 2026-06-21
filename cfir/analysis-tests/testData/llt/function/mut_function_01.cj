

struct S0 {
    var t0: Int64 = 0
    init() {}
}

struct S1 {
    var t1: S0 = S0()
    init() {}
}

struct S2 {
    var t2: S1 = S1()
    init() {}

    func f() {
        <!CANNOT_MODIFY_VAR!>t2<!>.t1.t0 += 10 // 非mut函数，不应该允许此类修改
    }
}

main(): Unit {
    let s2 = S2()
    s2.f()
    println(s2.t2.t1.t0.toString())
}
