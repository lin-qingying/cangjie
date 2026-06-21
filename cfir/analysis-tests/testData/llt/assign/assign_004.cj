

main(): Int64 {
    var a = [-1, 1, 2]
    a = <!TYPE_MISMATCH!>1000<!>
    return 0
}

var a = [-1, 1, 2]
var b = 1000

func foo() {
    var a = [-1, 1, 2]
    a = <!TYPE_MISMATCH!>1000<!>
}
