

func f(_: (Int64) -> Unit): Unit {}

func f(_: () -> Unit): Unit {}

let _ = f {
    let v = 1
    if (let v <- Some(v) && let Some(w) <- v) {
    }
}

main() {
}
