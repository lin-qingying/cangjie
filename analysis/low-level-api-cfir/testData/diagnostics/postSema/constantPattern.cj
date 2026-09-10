package test

func constantPattern(): Int64 {
    match (1) {
        case 0 => 0
        case _ => 1
    }
}
