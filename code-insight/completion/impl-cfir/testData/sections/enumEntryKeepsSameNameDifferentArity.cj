package completion.sections

enum Shape {
    | Dot
    | Dot(Int64)
    | Boxed(Int64)
}

func enumSite(): Int64 {
    Shape.completionTarget
}