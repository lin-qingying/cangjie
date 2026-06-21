

struct RA<T> {
    public RA(let a!: Int64 = 10) {}
}

main() {
    var ra = RA<Int64>(a: 5)
    if (ra.a == 5) {
        return 0
    }
    return 1
}
