func choose(c: Bool): Int32 {
    return if (c) { 1 } else { 2 }
}

main(): Int32 {
    return choose(true)
}

