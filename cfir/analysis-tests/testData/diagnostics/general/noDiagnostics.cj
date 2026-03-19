// RUN_PIPELINE_TILL: FRONTEND
func add(a: Int64, b: Int64): Int64 {
    return a + b
}

main() {
    let a: Int64 = 1
    let b: Int64 = 2
    let result = add(a, b)
}

