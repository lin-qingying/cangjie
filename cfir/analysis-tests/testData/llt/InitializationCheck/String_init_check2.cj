

main(): Int64 {
    var str1 = "cangjie"
    var str2 = "CangJie"
    var res1: String
    let iterable = str1
    for (i in iterable) {
        res1 = <!USED_BEFORE_INITIALIZATION!>res1<!> + i.toString()
    }

    if (<!USED_BEFORE_INITIALIZATION!>res1<!> == "228187147233162137") {
        return 0
    }

    return 1
}
