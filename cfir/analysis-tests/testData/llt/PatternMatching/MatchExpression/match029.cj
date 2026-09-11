



main() {
    let x = 0
    match (x) {
        case 0 => 0
        case _ => <!TYPE_INCOMPATIBLE!>match (x) {
            case 0 => 1
            case _ => "a"
        }<!>
    }
}
