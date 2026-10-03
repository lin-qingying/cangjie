// RUN_PIPELINE_TILL: FRONTEND
class E <: Exception {}

func f(b: Bool): Int64 {
    try {
        try {
            if (b) {
                throw E()
            }
            return 1
        } finally {
            let a = 1
            let _ = a
        }
    } catch (e: E) {
        return 2
    }
}
