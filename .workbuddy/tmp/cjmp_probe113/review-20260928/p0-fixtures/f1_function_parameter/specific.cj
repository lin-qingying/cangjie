package p0_f1_function_parameter

public specific func accept<U>(operation: (U) -> Unit, value: U): Unit {
    operation(value)
}
