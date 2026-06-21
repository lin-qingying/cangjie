

const str1: String = "hello"
var str2: String = "world"

const func test() : Unit {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>str1 += "."<!>
    <!EXPECT_CONST!>str2 += "!"<!>
}

main() {}
