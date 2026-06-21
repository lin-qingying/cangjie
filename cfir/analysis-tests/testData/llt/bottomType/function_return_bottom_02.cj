





main(): Int64 {
    case01()
    return 0
}

func case01() {
    while (<!INVALID_LOOP_CONTROL!>break<!>) {
        print("yes")
    }
}
