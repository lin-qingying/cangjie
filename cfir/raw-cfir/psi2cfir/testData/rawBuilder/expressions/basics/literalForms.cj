// RUN_PIPELINE_TILL: FRONTEND
func useLiterals(): Float64 {
    let c = 'x'
    let e = '\n'
    let a = 0x1F
    let b = 0b1010
    let d = 1.5e3
    let g = 1.0f64
    let _ = c
    let _ = e
    let _ = a
    let _ = b
    let _ = d
    return g
}
