package sample

@Flag(1)
class Annotated {}

struct Flag {
    const init(value: Int64) {}
}

class Box {}

func a(): Box {
    return Box()
}

func greet() {
}

struct Config {
    const init(value: Int64, label: Int64) {}
}

func b(): Int64 {
    let value: Box = a()
    let bound = 1
    let indexer = Indexer()
    indexer[0]
    let invokable = Invokable()
    invokable()
    greet()
    let span = 1..10
    let config = Config(value: 1, label: 2)
    return 0
}

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

class Base {
    init(value: Int64) {}
}

class Derived : Base(1) {}
