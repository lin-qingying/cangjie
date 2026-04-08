class Indexer {
    public operator func [](index: Int64): Int64 {
        return index
    }
}

class Invokable {
    public operator func ()(): Int64 {
        return 0
    }
}

class Delegating {
    init() {}

    init(value: Int64) {
        this()
    }
}

func useAll(): Int64 {
    let indexer = Indexer()
    let result = indexer[0]
    let invokable = Invokable()
    return invokable()
}
