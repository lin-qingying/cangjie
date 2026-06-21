

main(): Int64 {
    return 0
}

var a = 0
var f1: Unit = if (a > 0) {
    var a: Int32 = 1
    let b: Int32 = 2
    var x1 = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> a}<!>
    var x2 = {=> b}
    var x3 = {=> a}()
    var x4 = {=> b}()
}

func foo(x: () -> Int32) {
    return x
}

func goo(x: Int32) {
    return x
}
