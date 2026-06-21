


func test<T>(a: T): T where T <: ToString {
    a
}

main(): Int64 {
    return test(11)
}
