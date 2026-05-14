func abc3(): Unit {
    abc1 {
        _ =>
        return 1
    }
}

func abc2(): Unit {
    abc1 {
        _ =>
        1
    }
}

func abc1(a: (Int64) -> Int64) {
    let _ = a
}
