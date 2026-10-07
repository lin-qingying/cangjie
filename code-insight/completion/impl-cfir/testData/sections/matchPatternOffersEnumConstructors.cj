package completion.sections

enum Shape {
    | Dot
    | Boxed(Int64)
}

// match 按主语类型的枚举构造器分派，case 后面敲的几乎总是其中之一。
func describe(shape: Shape): Int64 {
    match (shape) {
        case completionTarget => 1
        case _ => 0
    }
}
