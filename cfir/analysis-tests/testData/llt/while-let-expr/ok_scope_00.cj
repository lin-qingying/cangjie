


func h(f: (Int64) -> Int64) {
    f(3)
}

main() {
    h {
        x =>
        var t = None<Int64>
        while (let Some(t) <- t) {
            println(t)
        }
        x
    }
}
