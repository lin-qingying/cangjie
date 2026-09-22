


func a() {
    let a = b()
    let c = <!UNRESOLVED_REFERENCE!>a1<!>()

    func a1() {
    }
}

func b() {
    123
}
