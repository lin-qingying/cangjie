func pLongFuncRef(): Unit {
    var v: Int64 = 1
    func longCapturingFunc(): Int64 {
        return v
    }
    func longCallerFunc(): Int64 {
        return longCapturingFunc()
    }
    let x = longCallerFunc
    let _ = x
    return
}