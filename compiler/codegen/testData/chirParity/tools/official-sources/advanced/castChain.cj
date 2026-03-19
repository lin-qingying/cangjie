// RUN_PIPELINE_TILL: FRONTEND
func idU32(x: UInt32): UInt32 {
    return x
}

main(): Int64 {
    let v: UInt32 = idU32(1)
    return Int64(v)
}


