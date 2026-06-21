


var a = 1

main(): Int64 {
    func g() {
        var x = {=> <!CAPTURE_HAS_SHADOW_VARIABLE!>a<!>}
    }
    let a = true
    return 0
}
