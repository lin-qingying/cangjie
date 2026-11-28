package sample.scope

class Container {
    let stored: Int64 = 0

    func use(): Int64 {
        return this.stored
    }
}

func consume(): Int64 {
    Container().use()
}
