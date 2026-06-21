

class A {}

class B {}

func Do1(a: Option<A>) {
    return match (a) {
        case Some(y) => 1
        case None => 0
    }
}

func Do2(a: Option<B>) {
    return match (a) {
        case Some(y) => 1
        case None => 0
    }
}

main(): Int64 {
    let a: Option<String> = Option.None
    if (Do1(None) != 0) {
        return 1
    }
    if (Do2(Option.None) != 0) {
        return 1
    }
    return 0
}
