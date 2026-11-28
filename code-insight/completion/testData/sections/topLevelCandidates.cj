package completion.sections

func helper(value: Int64): Int64 {
    value
}

func overloaded(value: Int64): Int64 {
    value
}

func overloaded(value: String): String {
    value
}

func consume(): Int64 {
    completionTarget
}
