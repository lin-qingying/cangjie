


func f<T>(a: Any, t: T): String {
    return ""
}

func f2<T>(a: Any, t: T): T {
    return t
}

main(): Int64 {
    f("", "")
    f2("", 0)
}
