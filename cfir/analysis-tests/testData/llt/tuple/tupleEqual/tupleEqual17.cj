

func foo(): (Int64, Int64) {
    return (1, 2);
}

main(): Int64 {
    let t1 = (1, 2);
    if (t1 != foo()) {
        return 1;
    }
    return 0;
}
