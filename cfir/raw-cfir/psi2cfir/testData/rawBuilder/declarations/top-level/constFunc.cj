// RUN_PIPELINE_TILL: FRONTEND
const func double(x: Int64): Int64 {
    return x * 2
}

func useConst(): Int64 {
    return double(2)
}
