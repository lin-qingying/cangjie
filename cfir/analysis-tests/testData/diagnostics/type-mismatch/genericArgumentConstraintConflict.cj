
func choose<T>(first: T, second: T): T {
    return first
}

main() {

    let value =  <!TYPE_MISMATCH!>choose(1, true)<!>
}
