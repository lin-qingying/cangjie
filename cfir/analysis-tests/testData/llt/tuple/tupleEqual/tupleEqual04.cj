

main(): Int64 {
    let t1: (Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64) = (1, 1, 1, 0, 1, 0, 0, 1);

    if (t1 == (1, 1, 1, 0, 1, 0, 0, 1)) {
        return 0;
    }

    return 1;
}
