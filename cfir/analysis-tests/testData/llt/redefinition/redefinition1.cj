


func doo() {}

func doo(i: Int64): Int32 {
    return 1
}

func <!CONFLICTING_OVERLOADS!>doo<!>(i: Int64): Int32 {
    return 1
}

func doo(i: Int32) {}

func doo(i: Float32) {}
