package sample.scope

enum Shape {
    | Circle(Int64)
    | Square(Int64)
}

func area(shape: Shape): Int64 {
    match (shape) {
        case Circle(radius) => radius
        case Square(side) => branchTarget
    }
}

main() {
    area(Circle(1))
}