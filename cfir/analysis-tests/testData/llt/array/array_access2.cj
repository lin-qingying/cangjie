

func includes(str: String, subString: String): Bool {
    return (str.indexOf(subString) ?? -1) >= 0
}

main() {
    let strArr = ["a"]
    let str = "abc"
    for (s in strArr where !includes(str, s)) {
        return 1
    }
    return 0
}
