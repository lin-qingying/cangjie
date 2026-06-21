

struct RA<T> {
    public let a = 10
    public init(a: Int32) {
        this.a = a
    }
}

main() {
    var ra = RA<Int64>(5)
    print("${ra.a}\n")
    return 0
}
