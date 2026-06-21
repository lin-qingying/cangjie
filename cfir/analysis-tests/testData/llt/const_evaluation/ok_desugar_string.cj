


const func mango() {
    "🥭"
}

const kiwi = "🥝"
const lemon = "🍋"

const func f() {
    mango() + kiwi == "🥭🥝" && mango() + lemon == "🥭🍋" && kiwi * 3 == "🥝🥝🥝" && "a" < "b" && "a" <= "b" && "b" > "a" &&
        "b" >= "a" && mango() != kiwi
}

main() {
    if (f()) {
        0
    } else {
        1
    }
}
