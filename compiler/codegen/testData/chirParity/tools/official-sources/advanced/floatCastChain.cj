func castMain(): Float64 {
    let v: Float32 = Float32(3)
    return -Float64(v)
}

main(): Int32 {
    return if (castMain() < 0.0) { 1 } else { 0 }
}
