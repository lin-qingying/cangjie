

func mango() {
    "🥭"
}

let kiwi = "🥝"
let lemon = "🍋"

main() {
    const x = <!EXPECT_CONST!>mango<!>() + <!EXPECT_CONST!>kiwi<!>
    const y = <!EXPECT_CONST!>mango<!>() + <!EXPECT_CONST!>lemon<!>
    const z = <!EXPECT_CONST!>kiwi<!> * 3
}
