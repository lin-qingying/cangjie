

let glo = 123

func a() {
    let d = <!UNRESOLVED_REFERENCE!>b<!>
    let glu = glo

    let b = "hello"
}
