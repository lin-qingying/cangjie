

main() {
    let _ = 'c' + unsafe {
        let _ = "${<!UNRESOLVED_REFERENCE!>x<!>}"
    }
}
