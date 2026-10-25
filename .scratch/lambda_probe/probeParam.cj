func transitiveTakeLambda(i: () -> Int64): Int64 {
    return i()
}

func transitiveCaptureLambdaCannotParam(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    return transitiveTakeLambda({=> f()})
}

class TransitiveArgTaker {
    func take(i: () -> Int64): Int64 {
        return i()
    }
}

func transitiveCaptureLambdaCannotParamAsMemberCallArg(): Int64 {
    var v: Int64 = 1
    func f(): Int64 {
        return v
    }
    let obj = TransitiveArgTaker()
    return obj.take({=> f()})
}