func choose(c: Bool): Int32 {
    return if (c) { 5 } else { 6 }
}

main(): Int32 {
    let neg: Int32 = -3
    let fneg: Float64 = -1.0
    let bitNot: Int32 = 0 ^ -1
    let lnot: Bool = !true
    let add: Int32 = 1 + 2
    let udiv: UInt32 = UInt32(8) / UInt32(2)
    let eq: Bool = 1 == 1
    let ult: Bool = UInt32(1) < UInt32(2)
    let fgt: Bool = 2.0 > 1.0
    let zext: Int64 = Int64(UInt32(1))
    let trunc: Int32 = Int32(Int64(9))
    let sitofp: Float32 = Float32(7)
    let fptosi: Int32 = Int32(Float32(7.0))
    return choose(true)
}

