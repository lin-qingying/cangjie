public enum Shape {
    | Circle
    | Square
}

public enum Color {
    | Red
    | Green
}

public enum Token {
    | Circle
    | Dot
}

public enum Box {
    | Wrap(Shape)
    | Empty
}

func foreignBare(shape: Shape): Int64 {
    match (shape) {
        case Green => 1
        case _ => 2
    }
}

func foreignNested(box: Box): Int64 {
    match (box) {
        case Wrap(Red) => 1
        case _ => 2
    }
}

func sharedNameOnShape(shape: Shape): Int64 {
    match (shape) {
        case Circle => 1
        case Square => 2
    }
}

func sharedNameOnToken(token: Token): Int64 {
    match (token) {
        case Circle => 1
        case Dot => 2
    }
}

main(): Int64 {
    return 0
}
