

func test(s: ToString) {
    if (s.toString() == "fail") {
        return 1
    } else {
        return 0
    }
}

main() {
    let v: ?Int64 = 1
    return test(v ?? "fail")
}
