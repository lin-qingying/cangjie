

extend Int64 {
    public func add(num: Int64): Int64 {
        return this + num
    }
    public func double(): Int64 {
        return add(this)
    }
}

main() {
    let x: Int64 = 1;
    return x.add(1) - x.double()
}
