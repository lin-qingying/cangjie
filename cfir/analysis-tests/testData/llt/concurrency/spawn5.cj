


func parallize<T>(initStatus: () -> T) {
    let f = spawn {
        =>
        var threadStatus = initStatus()
        return threadStatus
    }
}

func distanceAndPrune() {
    parallize<Int64>({=> 0})
}

main(): Int64 {
    return 0
}
