

var aa = 0

class Data<T> {
    public Data(var a!: Int64 = 6) {
        aa++
    }
    public func f2(): This {
        this
    }
    public func f1(a: T): This {
        if (true) {
            return f2()
        } else {
            return this
        }
    }
}

main(): Int64 {
    var data = Data<Int64>(a: 8)
    if (data.f1(3).a == 8 && aa == 1) {
        return 0
    }
    return 1
}
