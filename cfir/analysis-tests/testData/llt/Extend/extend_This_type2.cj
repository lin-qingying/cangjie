

interface ToStr {
    func toStr(): String
}

extend Int32 <: ToStr {
    public func toStr(): String {
        return this.toString()
    }
}

extend Int64 <: ToStr {
    public func toStr(): String {
        return this.toString()
    }
}

main() {
    let a: Int32 = 1
    print(a.toStr())
    0
}
