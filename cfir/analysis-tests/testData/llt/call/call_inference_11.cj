

let f = {x: String => x}

func f2<T, R>(x: T) {
    {x: R => x}
}

func f3(x: (String) -> Any) {
    x("s")
}

main() {
    let v: Any = f2("s")("s")
    f3(<!TYPE_MISMATCH!>f<!>)
    ()
}
