package sample.extendreceivers

extend Int64 {
    func doubled(): Int64 {
        return this * 2
    }
}

interface Shaped {
    func area(): Int64
}

class Box<T> {
    func unwrap(x: T): T {
        return x
    }
}

extend<T> Box<T> where T <: Shaped {
    func shapedTag(x: T): T {
        return x
    }
}

extend<T> CPointer<T> {
    func pointeeTag(): Int64 {
        return 1
    }
}

type RealBox = Box<Int64>

func takesInt64(value: Int64): Unit {
}

func takesBox(box: Box<Int64>): Unit {
}

func takesShapedBox(box: Box<Shaped>): Unit {
}

func takesBounded<T>(value: T): Unit where T <: Shaped {
}

func takesPointer(pointer: CPointer<Int64>): Unit {
}

func takesAlias(alias: RealBox): Unit {
}
