

func f() {
    print("func")
    {arr: Array<Int64> => arr.size}
}

func g() {
    print("arg")
    0
}

main() {
    g() |> f()
}
