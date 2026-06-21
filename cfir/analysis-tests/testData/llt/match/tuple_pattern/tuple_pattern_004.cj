

main(): Int64 {
    let scoreTuple = (("Allen", 80), ("Bob", 90))
    let allen = "Allen"
    let num80 = 80
    var scoreResult: String = match (scoreTuple) {
        case ((allen, num80), (bob, num90)) => "${bob} got ${num90}"
        case (_, _) => ""
    }
    print(scoreResult)
    0
}
