


class M {
    let x: Int64 = 1
}

func test(p: ?M) {
    spawn {
        while (true) {
            let value = p?.x ?? 0
        }
    }
}

main() {}
