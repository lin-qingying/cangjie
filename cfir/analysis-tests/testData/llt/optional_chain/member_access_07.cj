


extend Option<String> {
    func f() {
        this?.size
    }
}

main() {
    Some("abc").f() ?? 0
}
