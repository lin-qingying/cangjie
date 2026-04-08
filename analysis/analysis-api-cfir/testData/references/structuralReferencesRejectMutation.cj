package sample

class Indexer {
    public operator func [](index: Int64): Int64 {
        return index
    }
}

class Base {
    init(value: Int64) {}
}

class Derived : Base(1) {}

func useAll() {
    let indexer = Indexer()
    let result = indexer[0]
}
