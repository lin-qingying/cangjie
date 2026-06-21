

interface Number<T> {
    operator func +(right: T): T
}

interface Integer<T> <: Number<T> {}

extend Int32 <: Integer<Int64> & Number<Int8> & Integer<Int32> & Number<Int16> {
    public operator func +(right: Int64): Int64 {
        3
    }
    public operator func +(right: Int8): Int8 {
        4
    }
    public operator func +(right: Int16): Int16 {
        5
    }
}

main() {
    var x: Int32 = 1
    var a: Int64 = 1
    if (x + a != 3) {
        return 1
    }
    var b: Int8 = 1
    if (x + b != 4) {
        return 2
    }
    var c: Int32 = 1
    if (x + c != 2) {
        return 3
    }
    var d: Int16 = 1
    if (x + d != 5) {
        return 4
    }
    return 0
}
