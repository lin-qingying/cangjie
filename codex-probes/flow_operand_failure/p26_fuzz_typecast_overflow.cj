main() {
    let a: Int16 = Int16(UInt16(47))
    let b: Int64 = Int64(UInt8(254))
    let c: UInt = UInt64(190)
    let d: Float32 = Float32(Float32(Float32(-120.0)))
    let e: Int8 = Int8(Int8(-113)) & 127
    let f: Int64 = Int64(-91) | 1
    println("${a} ${b} ${c} ${d} ${e} ${f}")
}
