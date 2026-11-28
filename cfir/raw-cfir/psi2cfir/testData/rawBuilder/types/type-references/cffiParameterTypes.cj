// RUN_PIPELINE_TILL: FRONTEND
func use(f: CFunc<(Int64) -> Int32>, p: CPointer<UInt8>, s: CString, v: VArray<Int64, $4>): Unit {
    let _ = f
    let _ = p
    let _ = s
    let _ = v
}
