


var foo = 0
var bar = 0

main() {
    let x2 = Some<Int32>(42)
    let x3 = Option<Int32>.None
    let x4 = (1, 2, "hello")
    while (let (1, 2, "hello") <- x4) {
        foo = 1
        while (let (1, 3, "hello") <- x4) {
            foo = 0
            break
        }
        break
    }

    while (let _ <- x3) {
        bar = 1
        while (let Some(v) <- x3) {
            bar = 2
            break
        }
        while (let (2, 2, "hello") <- x4) {
            bar = 0
            break
        }
        break
    }

    if (foo != 1 || bar != 1) {
        return 1
    }

    0
}
