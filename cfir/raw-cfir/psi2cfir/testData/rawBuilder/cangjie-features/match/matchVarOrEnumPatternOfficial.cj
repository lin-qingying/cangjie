// RUN_PIPELINE_TILL: FRONTEND
enum Option<T> {
    Some(T)
    None
}

func matchOption(value: Option<Int64>): Int64 {
    return match (value) {
        case None => 0
        case Some(x) => x
        case fallback => 1
    }
}
