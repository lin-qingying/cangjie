class Box {
    init(value: Int64) {
        let _ = value
    }
}

class AmbiguousBox {
    init(value: Int64, left!: Int64 = 0) {}
    init(value: Int64, right!: Int64 = 0) {}
}

open class Base {
    init(value: Int64) {
        let _ = value
    }
}

class MissingSuper <: Base {
    init() {
    }
}

class IllegalSuper <: Base {
    init() {
        let _ = 1
        super(1)
    }
}

class Recursive {
    init() {
        this()
    }
}

class ThisOnly {
    init(value: Int64, again: Int64) {
        let _ = value
        let _ = again
    }

    init() {
        this(1)
    }
}

open class BaseTwo {
    init(value: Int64, again: Int64) {
        let _ = value
        let _ = again
    }
}

class Child <: BaseTwo {
    init() {
        super(1)
    }
}

interface Factory {}

func directConstruction(): Unit {
    let _ = Factory()
}

main(): Unit {
    Box()
    AmbiguousBox(1)
}
