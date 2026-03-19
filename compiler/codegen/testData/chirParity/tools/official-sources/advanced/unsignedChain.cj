// RUN_PIPELINE_TILL: FRONTEND
func unsignedMain(): Bool {
    let d: UInt32 = UInt32(9) / UInt32(2)
    let r: UInt32 = UInt32(9) % UInt32(2)
    let s: UInt32 = d + r
    return s >= UInt32(5)
}

main(): Int32 {
    return if (unsignedMain()) { 1 } else { 0 }
}

