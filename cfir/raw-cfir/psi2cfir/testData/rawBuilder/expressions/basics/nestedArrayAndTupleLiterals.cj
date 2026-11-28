// RUN_PIPELINE_TILL: FRONTEND
func useLiterals(): Unit {
    let a = [[1, 2], [3, 4]]
    let b: Array<Array<Int64>> = [[5]]
    let c = [(1, 2), (3, 4)]
    let _ = a
    let _ = b
    let _ = c
}
