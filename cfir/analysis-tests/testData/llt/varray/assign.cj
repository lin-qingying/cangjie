

var b = VArray<Int64, $3>({i => i})

func getb(n: String) {
    println(n)
    b
}

class A {
    var a = VArray<Int64, $3>({i => i})
    func foo() {
        a
    }
}

main() {
    let a = A()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.foo()[0] = 18<!>

    <!CANNOT_ASSIGN_TO_IMMUTABLE!>getb("left")[1] = 0<!>
}
