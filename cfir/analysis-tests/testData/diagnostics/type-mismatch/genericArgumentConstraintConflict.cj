// RUN_PIPELINE_TILL: FRONTEND
func choose<T>(first: T, second: T): T {
    return first
}

main() {
    let value = choose(1, <!ARGUMENT_TYPE_MISMATCH!>true<!>)
}
