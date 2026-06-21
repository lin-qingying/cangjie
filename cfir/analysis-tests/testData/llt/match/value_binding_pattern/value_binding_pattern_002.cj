

main():Int64 {
    let a = 10
    var b = match (a) {
        case 11 => 1
        case c@_ => 2
    }
    return 0
}
