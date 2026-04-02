
func pair<T>(left: T, right: T): T {
    return left
}

main() {
    let number: Int64 = <!TYPE_MISMATCH!>pair(1, "oops")<!>

}
