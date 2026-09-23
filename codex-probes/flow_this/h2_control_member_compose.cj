class A {
    var x = 1
    func f(v: Int64): Int64 { v }
    func g(): Unit {
        x ~> f
    }
}
main() {}
