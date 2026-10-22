package test

class Counter {
    private var value: Int64 = 0

    private func bump(): Unit {
        value = value + 1
    }

    func tick(): Unit {
        bump()
        value = value + 1
    }
}
