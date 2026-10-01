// RUN_PIPELINE_TILL: FRONTEND
main() {
    let f = <expr>{ a => a + 1 }</expr>
    let _ = f
    0
}
