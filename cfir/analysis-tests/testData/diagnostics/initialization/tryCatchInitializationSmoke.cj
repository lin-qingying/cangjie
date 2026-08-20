// RUN_PIPELINE_TILL: FRONTEND
class ValueHolder {
    var value: Int64

    init(flag: Bool) {
        try {
            if (flag) {
                value = 1
            } else {
                value = 2
            }
        } catch (e: Exception) {
            value = 3
        } finally {
            let _ = true
        }
    }
}

func localInitThroughTryCatch(flag: Bool): Int64 {
    var value: Int64
    try {
        if (flag) {
            value = 1
        } else {
            value = 2
        }
    } catch (e: Exception) {
        value = 3
    } finally {
        let _ = true
    }
    return value
}
