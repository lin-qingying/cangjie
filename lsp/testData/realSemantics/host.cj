package sample.real

interface Greeter {
    func greet(name: String): String
}

class Host <: Greeter {
}
