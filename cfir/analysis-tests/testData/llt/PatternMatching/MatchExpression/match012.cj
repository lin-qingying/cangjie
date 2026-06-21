

main(): Int64 {
    var result = match (50) {
        case 60 => "pass"
        case 70 | 80 =>
            "yes";
            var a = 1;
            "good"
        case other =>
            "yes";
            var test: (Int64) -> String = {x: Int64 => x.toString()};
            test(other - 1)
    }
    if (result == "49") {
        return 0
    }
    return 1
}
