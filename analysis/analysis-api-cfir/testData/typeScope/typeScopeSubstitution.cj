package sample.typeScope

interface Shaped {
    func area(): Int64
}

interface Container<T> {
    prop stored: T
}

open class Box<T> {
    func unwrap(x: T): T {
        return x
    }
}

class IntBox <: Box<Int64> {
}

type IntBoxAlias = Box<Int64>

extend<T> Box<T> where T <: Shaped {
    func shapedTag(x: T): T {
        return x
    }
}

func takesIntBox(box: Box<Int64>): Unit {
}

func takesShapedBox(box: Box<Shaped>): Unit {
}

func takesIntBoxChild(child: IntBox): Unit {
}

func takesIntBoxAlias(alias: IntBoxAlias): Unit {
}

func takesContainer(container: Container<Int64>): Unit {
}

func takesBounded<T>(value: T): Unit where T <: Shaped {
}
