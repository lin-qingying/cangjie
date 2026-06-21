

func h(f: (Int64) -> Int64) {
    f(3)
}

main() {
    h {
        x =>
        if (let Some(x) <- Some(x)) {
            println(x)
        }
        0
    }
}
