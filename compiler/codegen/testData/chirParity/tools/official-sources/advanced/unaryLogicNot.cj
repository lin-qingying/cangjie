func logicNotMain(): Bool {
    return !true
}

main(): Int32 {
    return if (logicNotMain()) { 1 } else { 0 }
}
