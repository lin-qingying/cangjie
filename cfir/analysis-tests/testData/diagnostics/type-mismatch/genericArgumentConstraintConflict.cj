// RUN_PIPELINE_TILL: FRONTEND
// DUMP_INFERENCE_LOGS
// DUMP_SCOPE
// VERIFY_RESOLVED_TYPES
func choose<T>(first: T, second: T): T {
    return first
}

main() {
    let value = choose(1, <!ARGUMENT_TYPE_MISMATCH!>true<!>)
}
