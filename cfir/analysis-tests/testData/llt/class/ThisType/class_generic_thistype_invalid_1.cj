

var aa = 0

class Data<T> {
    public Data(var a!: Int64 = 6) {
        aa++
    }

    public func f1(a: T): This {
        func f2() {
            this
        }
        f2()
    }
}

main(): Int64 {
    var data = Data<Int64>(a: 8)
    if (data.f1(3).a == 8 && aa == 1) {
        return 0
    }
    return 1
}
