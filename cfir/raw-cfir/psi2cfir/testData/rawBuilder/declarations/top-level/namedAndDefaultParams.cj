// RUN_PIPELINE_TILL: FRONTEND
func f(a: Int64, b!: Int64 = 2): Int64 {
    return a + b
}

func use() {
    let _ = f(1)
    let _ = f(1, b: 3)
}
