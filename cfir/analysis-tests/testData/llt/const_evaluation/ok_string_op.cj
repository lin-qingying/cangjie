


const func test() {
    let s = "test"
    s == "test" && s != "tess"
    s + " it" == "test it" && s + "s" == "tests" && s * 3 == "testtesttest" && s < "tesz" && s <= "tesz" && s > "tess" &&
        s >= "tess"
}

main() {
    if (test()) {
        0
    } else {
        1
    }
}
