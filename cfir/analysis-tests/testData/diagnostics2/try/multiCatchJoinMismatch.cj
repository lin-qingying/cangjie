// RUN_PIPELINE_TILL: FRONTEND
class ExA <: Exception {}
class ExB <: Exception {}

func multiCatchJoinMismatch(flag: Bool): Int64 {
    return try {
        if (flag) {
            throw ExA()
        }
        1
    } catch (e: ExA) {
        2
    } catch (e: ExB) {
        <!TYPE_MISMATCH!>false<!>
    }
}
