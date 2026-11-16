package sample.watch

class MainType {
    func ping(): Int64 {
        return 1
    }
}

func use(): Int64 {
    let instance = MainType()
    return instance.ping()
}
