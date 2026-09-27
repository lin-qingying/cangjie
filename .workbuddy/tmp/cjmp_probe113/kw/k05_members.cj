package k05

class A {
    var common: Int64 = 1
    func specific(): Int64 { common }
}

main(): Int64 {
    let a = A()
    a.common = 2
    a.specific()
}
