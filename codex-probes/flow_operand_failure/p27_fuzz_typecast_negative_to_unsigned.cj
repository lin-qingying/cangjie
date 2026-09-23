main() {
    let a: UInt64 = UInt64(Int8(-82))
    let b: UInt64 = UInt64(Int16(-120))
    let c: UInt32 = UInt32(Int32(Int16(-6)))
    let d: UInt64 = UInt64(Int64(-65))
    let e: UInt16 = UInt16(Int8(-26))
    let g: Int16 = Int16(Int16(UInt8(Int8(-89))))
    let h: Int8 = Int8(UInt8(130))
    println("${a}${b}${c}${d}${e}${g}${h}")
}
