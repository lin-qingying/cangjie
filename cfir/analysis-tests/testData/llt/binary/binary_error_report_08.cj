

main(): Int64 {
    const x: Int8 = -20;
    1 + (x / (7 | 1) + (if (true) {
        x
    } else {
        0
    } + 0));
    return 0;
}
