

enum Result {
    Abort | Continue | Retry
}

func visit(handler: (String) -> Unit) {
    2
}

func visit(handler: (Int64) -> Result) {
    1
}

main() {
    visit {s => return Result.Abort}
}
