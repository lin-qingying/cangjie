

func test<T>() {
    let a = {
        =>
        var zz: ?T = None
        if (let None <- zz) {
            return Option<Any>.None
        }
        zz.getOrThrow()
    }
    ()
}

main() {
    test<String>()
    return 0
}
