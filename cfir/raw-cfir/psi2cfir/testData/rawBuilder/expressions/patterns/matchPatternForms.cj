// RUN_PIPELINE_TILL: FRONTEND
enum Shape {
    | Circle(Int64)
    | Rect(Int64, Int64)

    func selfArea(): Int64 {
        return match (this) {
            case Circle(r) => r
            case Rect(w, h) => w * h
        }
    }
}

func area(s: Shape): Int64 {
    return match (s) {
        case Circle(r) => r
        case Rect(w, h) => w * h
    }
}

func guarded(s: Shape): Int64 {
    return match (s) {
        case Circle(r) where r > 10 => 2
        case Circle(_) => 1
        case _ => 0
    }
}

func orPattern(x: Int64): Int64 {
    return match (x) {
        case 1 | 2 => 1
        case _ => 0
    }
}

func tuple(t: (Int64, String)): Int64 {
    return match (t) {
        case (a, b) => a
    }
}

enum E {
    | A
    | B(Int64)
}

func varOrEnum(e: E): Int64 {
    return match (e) {
        case E.A => 1
        case E.B(x) => x
    }
}
