func g(v: Int64): Int64 { v }

class C {
    var x = 1
    func f() { x ~> g }
}

main() {}
