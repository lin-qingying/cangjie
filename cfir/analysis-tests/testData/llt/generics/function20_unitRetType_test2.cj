

// DIAGNOSTICS: -UNUSED_VARIABLE

public class A<T> {
    public static var a = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>{input: T => input}<!>
    public static var b = <!STATIC_VARIABLE_USE_GENERIC_PARAMETER!>{input: T => input}<!>
}

main(): Int64 {
    1 |> A<Int64>.a
    A<Int64>.a ~> A<Int64>.b
    1 |> (A<Int64>.a ~> A<Int64>.b)
    return 0
}
