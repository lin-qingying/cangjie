


class E {
    public func fn() {
        println("hello")
    }
}

class F {}

func test(t: Object): Any {
    if (t is E) {
        return t as E
    } else {
        return t as F
    }
}

main() {
    let x = test(E()) as Option<E>

    match (x) {
        case Some(Some(e)) => e.fn()
        case _ => ()
    }
}
