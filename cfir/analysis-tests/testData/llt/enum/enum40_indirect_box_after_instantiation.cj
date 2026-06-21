

func foo<T>(v: T): ?Any {
    v
}

main(): Int64 {
    let v: ?Exception = None
    let v2 = foo(v)
    match (v2) {
        case Some(v: Option<Exception>) => match (v) {
            case None => 0
            case _ => 1
        }
        case _ => 1
    }
}
