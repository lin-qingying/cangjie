


class A {
    public var x: Int64 = 0
    public operator func [](index: Int64, value!: Int64): Unit {
        this.x = index + value
    }
}

main(): Int64 {
    let a = A();
    a[2] = 5
    if (a.x != 7) {
        return 1;
    }
    return 0;
}
