

func goo(x: Int64, a: () -> Int64) {
    a()
}

func test(): (Int64, Int64) {
    (1, 1)
}

func other() {
    let v1 = goo(1) {
        var (v1, v2) = test()
        return v1
    }
}

main() {
    goo(2) {
        for (i in 0..1) {
            let v1 = goo(1) {
                var (v1, v2) = test()
                return v1
            }
        }
        0
    }
    0
}
