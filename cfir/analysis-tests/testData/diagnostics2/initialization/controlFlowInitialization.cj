// RUN_PIPELINE_TILL: FRONTEND
func localReadAfterWhile(flag: Bool): Int64 {
    let value: Int64
    while (flag) {
        value = 1
        break
    }
    return <!USED_BEFORE_INITIALIZATION!>value<!>
}

func localReadAfterDoWhile(flag: Bool): Int64 {
    let value: Int64
    do {
        value = 1
    } while (flag)
    return value
}

func localReadAfterTryCatch(flag: Bool): Int64 {
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

func localReadAfterTryWithoutInitialization(): Int64 {
    let value: Int64
    try {
        let copy = 1
    } catch (e: Exception) {
        let fallback = 2
    } finally {
        let done = true
    }
    return <!USED_BEFORE_INITIALIZATION!>value<!>
}
