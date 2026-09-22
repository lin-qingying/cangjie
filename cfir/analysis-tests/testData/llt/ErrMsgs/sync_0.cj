



main() {
    let b = synchronized(<!USE_EXPR_WITHOUT_IMPORT!>()<!>) {}
    let c: Bool = synchronized(<!USE_EXPR_WITHOUT_IMPORT!>false<!>) {
        <!CANNOT_CONVERT_LITERAL!>1<!>
    }
    0
}

