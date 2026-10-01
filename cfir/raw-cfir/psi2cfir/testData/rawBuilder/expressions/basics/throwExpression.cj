// RUN_PIPELINE_TILL: FRONTEND
class MyError <: Exception {}

func mayThrow(flag: Bool): Int64 {
    if (flag) {
        throw MyError()
    }
    return 0
}
