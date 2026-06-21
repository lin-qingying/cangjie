


func a() {
    let a = b()
    let c = <!UNRESOLVED_REFERENCE!>a<!>1()

    func a1() {
    }
}

func b() {
    123
}
