// RUN_PIPELINE_TILL: FRONTEND
class FirstEx <: Exception {}
class SecondEx <: Exception {}

func mismatchAcrossMultipleCatches(flag: Int64): Int64 {
    return try {
        if (flag == 0) {
            throw FirstEx()
        }
        if (flag == 1) {
            throw SecondEx()
        }
        1
    } catch (e: FirstEx) {
        2
    } catch (e: SecondEx) {
        <!TYPE_MISMATCH!>false<!>
    }
}

func compatibleAcrossMultipleCatches(flag: Int64): Int64 {
    return try {
        if (flag == 0) {
            throw FirstEx()
        }
        if (flag == 1) {
            throw SecondEx()
        }
        1
    } catch (e: FirstEx) {
        2
    } catch (e: SecondEx) {
        3
    }
}
