

func g<A, B>(a: A): B {
    let b = g<A, B>(a)
    return b
}

main(): Int64 {
    return g<Bool, Int64>(false);
}
