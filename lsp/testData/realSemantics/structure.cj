package sample.structure

interface Shape {
    func area(): Int64
}

class Square <: Shape {
    var side: Int64 = 2

    override func area(): Int64 {
        return side * side
    }
}
