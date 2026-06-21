

main(): Int64 {
    var result = match {
        case false => "pass"
        case 2 < 1 =>
            "yes";
            var a = 1;
            "good"
        case _ =>
            "yes";
            var test: (Int64) -> String = {x: Int64 => x.toString()};
            test(50 - 1)
    }
    if (result == "49") {
        return 0
    }
    return 1
}
