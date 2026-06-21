

class Data<T1> where T1 <: ToString {
    public let a: T1
    public Data(x: T1) {
        a = x
    }
    public func show() {
        print("a = ${a}\n")
    }
}

main() {
    let data: Data<Int32> = Data(3)
    data.show()
    return 0
}
