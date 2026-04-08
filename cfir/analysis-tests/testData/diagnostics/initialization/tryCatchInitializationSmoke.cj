// RUN_PIPELINE_TILL: FRONTEND
class ValueHolder {
    let value: Int64

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
            let done = true
        }
    }
}

func localInitThroughTryCatch(flag: Bool): Int64 {
    let value: Int64
    try {
        if (flag) {
            value = 1
        } else {
            value = 2
        }
    } catch (e: Exception) {
        value = 3
    } finally {
        let done = true
    }
    return value
}
