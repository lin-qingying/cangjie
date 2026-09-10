let (selfTuple, _) = (<!UNRESOLVED_REFERENCE!>selfTuple<!> = 1)
let selfValue: Int64 = <!UNRESOLVED_REFERENCE!>selfValue<!>
let selfLambda: () -> Int64 = { => <!UNRESOLVED_REFERENCE!>selfLambda<!>() }
let nestedSelf: () -> Int64 = { =>
    let intermediate = <!UNRESOLVED_REFERENCE!>nestedSelf<!>()
    intermediate
}

let x: Int64 = 9

func shadowDirect(): Int64 {
    let x = x
    x
}

func shadowTuple(): Int64 {
    let (x, second) = (x, 2)
    x + second
}

func lambdaCapture(): Int64 {
    let source = 4
    let f: () -> Int64 = { => source }
    f()
}

func lambdaShadow(): Int64 {
    let x: () -> Int64 = { => <!CAPTURE_HAS_SHADOW_VARIABLE!>x<!> }
    x()
}
