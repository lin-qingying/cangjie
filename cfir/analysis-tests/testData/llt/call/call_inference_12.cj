

func f<A, B, C>(a: A): (B) -> (C) -> Unit {
    {b: B => {c: C => ()}}
}

main() {
    let v = (((f(1)))(2))(3)
}
