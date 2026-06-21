

func g(f: (Int) -> Int) {}

func g(f: (String) -> String) {}

main() {
    let _ = g(
        {
            x =>
            let v1 = x
            let v2 = v1
            g({y => v2})
            x.toString()
        }
    )
}
