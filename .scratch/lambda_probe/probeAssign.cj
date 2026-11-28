func transitiveCaptureLambdaCannotAssign(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    let x = {=> f()}
    return x()
}

func transitiveCaptureLambdaCannotReturn(): () -> Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    return {=> f()}
}

func transitiveCaptureLambdaCannotExpr(): Unit {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    {=> f()}
    return
}