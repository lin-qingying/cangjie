


func foo(): Int64 {
    let v = spawn {
        var c = 11
        {
            =>
            c = 0
            c
        }()
    }
    return v.get()
}

main(): Int64 {
    return foo()
}
