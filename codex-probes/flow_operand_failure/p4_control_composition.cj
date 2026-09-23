func g(v: Int64): Int64 { v }

main() {
    let r = g ~> g
    r(1)
}
