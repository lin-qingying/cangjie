


func f(b: Byte) {
    match (b) {
        case 97 => "A"
        case b'b' => "B"
        case _ => "?"
    }
}

main() {
    if (f(b'a') != "A") {
        1
    } else if (f(b'b') != "B") {
        2
    } else {
        0
    }
}
