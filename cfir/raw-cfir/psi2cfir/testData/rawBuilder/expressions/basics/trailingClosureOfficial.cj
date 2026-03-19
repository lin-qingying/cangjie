// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
func f(a: Int64, fn: (Int64) -> Int64) {
    fn(a)
}

func f2(fn: (Int64) -> Int64) {
    fn(1)
}

func useClosures() {
    f(1, {i => i * i})
    f(1) {i => i * i}
    f2 {i => i * i}
}
