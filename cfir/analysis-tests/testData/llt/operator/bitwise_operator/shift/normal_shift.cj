

main(): Int64 {
    let a: Int64 = 10 << 1 << 1
    if (a != 40) {
        return 1
    }
    let b = -30 << 2
    if (b != -120) {
        return 1
    }
    let c = -30 >> 2
    if (c != -8) {
        return 1
    }
    return 0;
}
