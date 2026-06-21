


func f() {
    print("func")
    {_: Int64 => 0}
}

func g() {
    print("arg")
    0
}

main() {
    g() |> f()
}
