

main(): Int64 {
    let t1 = (1, 2, 3);

    if (t1 != (1, 2, 3)) {
        return 1;
    }
    if (t1 == (1, 3, 3)) {
        return 2;
    }
    return 0;
}
