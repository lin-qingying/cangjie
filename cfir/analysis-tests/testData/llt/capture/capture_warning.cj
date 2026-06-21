


let x = 4

private func f() {
    func f1() {
        <!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>
    }
    let a = {
        => <!CAPTURE_HAS_SHADOW_VARIABLE!>x<!>
    }
    let x = 2
}

main() {
    0
}
