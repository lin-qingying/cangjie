


<!TYPEALIAS_UNUSED_TYPE_PARAMETERS!>type str<T><!> = String

main(): Int64 {
    var str1: str<?Int32> = "hello"
    var str2: String = "Rune"
    var res: str<Int32> = str1 + " " + str2
    if (res == "hello Rune") {
        return 0
    }
    return 1
}
