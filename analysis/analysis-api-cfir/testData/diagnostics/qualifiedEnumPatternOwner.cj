public enum Shape {
    | Circle
    | Square(Int64)
}

public enum Slot<T> {
    | Free
    | Used(T)
}

public type ShapeAlias = Shape

public type IntSlot = Slot<Int64>

func ownerIsEnum(shape: Shape): Int64 {
    match (shape) {
        case Shape.Circle => 1
        case Shape.Square(side) => side
    }
}

func ownerIsAlias(shape: Shape): Int64 {
    match (shape) {
        case ShapeAlias.Circle => 1
        case ShapeAlias.Square(side) => side
    }
}

func ownerIsGenericEnum(slot: Slot<Int64>): Int64 {
    match (slot) {
        case Slot<Int64>.Free => 0
        case IntSlot.Used(value) => value
    }
}

main(): Int64 {
    return 0
}
