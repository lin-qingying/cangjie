package sample.scope

func overloaded(value: Int64): Int64 {
    value
}

func overloaded(value: String): String {
    value
}

func consume(value: Int64): Int64 {
    overloaded(value)
    return value
}
