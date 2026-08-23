



main() {
    // float literal exceeds the value range of target float type.
    let min16: Float16 = 5.960464477539063E-8
    let small16: Float16 = 5.9604E-8
    let max16: Float16 = 6.5504E4
    let large16: Float16 = 6.5540E4

    let min32: Float32 = 1.40129846E-45
    let small32: Float32 = 0.6012E-45
    let max32: Float32 = 3.40282347E38
    let large32: Float32 = 3.4029E38

    let min64: Float64 = 4.9406564584124654E-324
    let small64: Float64 = 2.4625E-324
    let max64: Float64 = 1.7976931348623157E308
    let large64: Float64 = 1.7977E308
    var lit01: Float64 = 2.10e-100000

    // float literal exceeds the supported parsable value range.
    let small = 1E-10000
    let large = 1E10000

    let _ = 1_e1000000000000000000000000000000000
    let _ = 0x1p-100000000000000000000000000000000000000000
}

