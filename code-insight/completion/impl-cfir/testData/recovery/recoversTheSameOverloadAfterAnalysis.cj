package recoveryFixture

func overload(value: Int64): Int64 {
    value
}

func overload(value: String): String {
    value
}

func consume(): Int64 {
    completionTarget
}
