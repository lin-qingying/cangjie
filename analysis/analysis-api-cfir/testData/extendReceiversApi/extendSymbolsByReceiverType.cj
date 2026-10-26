package sample.extendreceiversapi

extend Int64 {
    func doubled(): Int64 {
        return this * 2
    }
}

extend<T> CPointer<T> {
    func pointeeTag(): Int64 {
        return 1
    }
}

class Box<T> {
}

type RealBox = Box<Int64>

func takesInt64(value: Int64): Unit {
}

func takesPointer(pointer: CPointer<Int64>): Unit {
}

func takesAlias(alias: RealBox): Unit {
}
