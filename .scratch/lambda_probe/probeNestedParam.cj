func lambdaParamWithoutTypeInsideCapturedLambda(): Unit {
    let f = {i: Int64 =>
        let g = {j => j}
        g(i)}
    f(1)
    return
}

func lambdaParamWithoutTypeAnnotation(): Unit {
    let f = {i => i}
    return
}