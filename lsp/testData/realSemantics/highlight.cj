package sample.kind

class Counter {
    var value: Int64 = 0

    func bump(): Unit {
        value = 1
    }

    func read(): Int64 {
        return value
    }
}
