struct S0 {
    var t0 = 0
    var t1 = {=> t0 += 1}
}

main() {
    let s0 = S0()
    s0.t0
}
