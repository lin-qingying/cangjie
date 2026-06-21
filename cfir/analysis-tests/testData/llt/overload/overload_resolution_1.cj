

class ConsTest {
    public static func f(a: Bool): Unit {}
    public static func f(): Unit {}
}

func g(a: Bool): Unit {}

func g(): Unit {}

let a: (Bool) -> Unit = g // OK
let b: () -> Unit = ConsTest.f // OK
let c: (Bool) -> Unit = ConsTest.f // OK
let d = ConsTest.<!AMBIGUOUS_USE!>f<!>
let lst = [(ConsTest.<!AMBIGUOUS_USE!>f<!>, 1), (ConsTest.<!AMBIGUOUS_USE!>f<!>, 2)]

main(): Int64 {
    let x = lst[0][0]
    x + 1
    0
}
