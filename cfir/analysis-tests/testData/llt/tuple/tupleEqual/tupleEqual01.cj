

main(): Int64 {
    let t1 = (1, 2, 3);
    let t2 = (1, 2, 3);
    let t3 = (1, 2, 4);

    if (t1 != t2) {
        return 1;
    }
    if (t1 == t3) {
        return 2;
    }
    return 0;
}
