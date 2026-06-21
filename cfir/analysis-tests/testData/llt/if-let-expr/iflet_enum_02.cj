

func foo(): Option<Int64> {
    let x1 = Option<Int64>.Some(111)
    let x2 = Option<Int64>.None
    if (let Some(v) <- x1) {
        v
    } else if (let Some(v) <- x2) {
        v
    } else {
        None
    }
}

main() {
    let a = foo()
    if (let Some(v) <- a) {
        if (v != 111) {
            return 1
        }
    }
    0
}
