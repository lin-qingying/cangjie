

main() {
    let v: Int64 = if (match {
        case _ =>
            <!NO_MATCHING_OPERATOR_INVOKE!>Option<Unit>.None<!>()
            true
    }) {
        1
    } else {
        2
    } + 1
    return 0
}
