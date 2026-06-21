


main(): Int64 {
    return case01()
}

func case01() {
    let a = 0..10 : 1
    let b = 0..10 : 1
    var result = match (a) {
        case b => 0
        case _ => 1
    }
    return result
}
