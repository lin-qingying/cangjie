package completion.expectedtype

func overload(value: Int64): Int64 {
    value
}

func overload(value: String): String {
    value
}

func ambiguousCandidatesYieldNoExpectedType() {
    overload(missing())
}
