

interface ToStringI {
    func ToString(): String
}

extend Int64 <: ToStringI {
    public func ToString(): String {
        return this.toString()
    }
}

class A {}

extend A <: ToStringI {
    public func ToString(): String {
        return "A"
    }
}

main(): Int64 {
    print(123.ToString())
    print(A().ToString())
    return 0
}
