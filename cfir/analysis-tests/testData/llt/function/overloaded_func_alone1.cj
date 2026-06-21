

main(): Int64 {
    var a = <!AMBIGUOUS_USE!>foo<!>
    var b: Int64 = match (<!AMBIGUOUS_USE!>foo<!>) {
        case x: () -> Unit => 1
        case x: (Float32) -> Int64 => 0
        case x: (Float64) -> Int64 => 3
        case _ => 4
    }
    if (b != 0) {
        return 1
    }
    <!AMBIGUOUS_USE!>foo<!>
    var c = match {
        case true => <!AMBIGUOUS_USE!>foo<!>
        case _ => <!AMBIGUOUS_USE!>foo<!>
    }
    return 0
}

public func foo(x: Float32) {
    return Int64(x)
}

public func foo(x: Float64) {
    return Int64(x)
}

public func foo(x: Float64, y: String) {
    return Int64(x)
}
