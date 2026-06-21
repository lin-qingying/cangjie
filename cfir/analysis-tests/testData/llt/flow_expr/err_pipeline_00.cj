

func f(x: Array<Int>) {
    x
}

main(): Int {
    <!TYPE_MISMATCH!>0 |> f<!>
}
