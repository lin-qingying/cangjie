

class A {
    var a = 111
}

func test(): Option<Array<Option<Option<A>>>> {
    Array(1, repeat: A())
}

main() {
    if (let Some(x) <- test()) {
        if (!x.isEmpty()) {
            if (let Some(Some(v)) <- x[0]) {
                if (v.a == 111) {
                    return 0
                }
            }
        }
    }
    return 1
}
