

func r1(a: Future<Unit>): Unit {
    r1(spawn {
        =>
        if (true) {
            return ()
        } else {
            return
        };
        true
    })
    true
}

func r2(a: Future<Unit>): Unit {
    r2(spawn {
        =>
        if (true) {
            false
        };
        true
    })
    true
}

func r3(a: Future<Int8>): Unit {
    r3(spawn {
        =>
        if (true) {
            false
        };
        1
    })
    true
}

main() {
    0
}
