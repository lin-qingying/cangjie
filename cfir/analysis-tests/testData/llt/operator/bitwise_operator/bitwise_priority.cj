

main(): Int64 {
    var a1 = 1 | 2 ^ 3
    var b1 = 1 | (2 ^ 3)
    if (a1 != b1) {
        return 1;
    }
    var a2 = 1 ^ 2 & 3
    var b2 = 1 ^ (2 & 3)
    if (a2 != b2) {
        return 1;
    }
    var a3 = 1 & 2 << 3
    var b3 = 1 & (2 << 3)
    if (a3 != b3) {
        return 1;
    }
    var a4 = 1 & 2 >> 3
    var b4 = 1 & (2 >> 3)
    if (a4 != b4) {
        return 1;
    }
    var a5 = 1 << !(-3)
    var b5 = 1 << (!(-3))
    if (a5 != b5) {
        return 1;
    }
    var a = (7 & 6) | (5 ^ (4 & ((3 << 2) >> (!(-1)))))
    var b = 7 & 6 | 5 ^ 4 & 3 << 1 >> !(-1)
    if (a != b) {
        return 1;
    }
    return 0;
}
