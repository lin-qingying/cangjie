

var ae: Array<Int32> = [11]

func test(): Option<Array<Int32>> {
    try {
        ae
    } catch (e: Exception) {
        ae
    }
}

main() {
    if (let Some(x) <- test()) {
        if (!x.isEmpty() && x[0] == 11) {
            return 0
        }
    }
    return 1
}
