

func function26(
    param37!: Bool = {
        =>
        let a: Bool = true
        func function41(param42!: Bool = a): Bool {
            param42
        }
        function41();
    }()): Bool {
    param37;
}

main(): Int64 {
    if (function26() != true) {
        return 1;
    }
    return 0;
}
