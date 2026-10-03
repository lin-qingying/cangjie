// RUN_PIPELINE_TILL: FRONTEND
class E1 <: Exception {}
class E2 <: Exception {}

func f(b: Bool): Int64 {
    try {
        if (b) {
            throw E1()
        }
        return 1
    } catch (e: E1) {
        return 2
    } catch (e: E2) {
        return 3
    } finally {
        let a = 1
        let _ = a
    }
}
