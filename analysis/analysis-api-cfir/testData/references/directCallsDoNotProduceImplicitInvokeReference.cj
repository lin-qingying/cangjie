package sample

func greet() {
}

class Invokable {
    public operator func ()(): Int64 {
        return 0
    }
}

func useCalls() {
    greet()
    let invokable = Invokable()
    invokable()
}
