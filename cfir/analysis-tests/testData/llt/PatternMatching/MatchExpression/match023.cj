

// DIAGNOSTICS: -UNUSED_VARIABLE

main() {
    let x = Some(true)
    match (x) {
        <!DIFFERENT_OR_PATTERN!>case Some(true) | false | 0 => 0<!>
        case Some(false) => 1
        case _ => 2
    }
}
