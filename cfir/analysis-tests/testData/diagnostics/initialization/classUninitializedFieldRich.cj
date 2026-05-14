// RUN_PIPELINE_TILL: FRONTEND
class CompleteByInitializer {
    let value: Int64 = 1

    init() {
    }
}

class CompleteByConstructor {
    let value: Int64

    init() {
        value = 1
    }
}

class MissingField {
    let value: Int64

    <!CLASS_UNINITIALIZED_FIELD!>init<!>() {
    }
}

class MissingAfterPartialBranch {
    let value: Int64

    <!CLASS_UNINITIALIZED_FIELD!>init<!>(flag: Bool) {
        if (flag) {
            value = 1
        }
    }
}

class DelegatedCtor {
    let value: Int64

    init(flag: Bool) {
        if (flag) {
            value = 1
        } else {
            value = 2
        }
    }

    init() {
        this(true)
    }
}
