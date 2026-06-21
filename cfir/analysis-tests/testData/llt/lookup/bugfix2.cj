
internal import <!UNRESOLVED_IMPORT!>stdx<!>.encoding.json.*
main() {
    let result1: JsonValue = JsonValue.fromStr(#"{"key1": "value1"}"#)
    let result2 = JsonValue.fromStr(#"{"key2": "value2"}"#)
    println("${result1}" +  "${result2}")
    return 0
}


