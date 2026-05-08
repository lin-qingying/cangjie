// RUN_PIPELINE_TILL: FRONTEND
enum Option<T> {
    Some(T) | None
}

func tuplePatternOnInt(v: Int64): Int64 {
    return match (v) {
        case <!TYPE_MISMATCH!>(a, b)<!> => 0
    }
}

func constPatternOnEnum(v: Option<Int64>): Int64 {
    return match (v) {
        case <!TYPE_MISMATCH!>1<!> => 0
        case Some(value) => value
        case None => 2
    }
}

func enumPatternWrongArity(v: Option<Int64>): Int64 {
    return match (v) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Some<!>(value, extra) => 0
        case Some(value) => value
        case None => 1
    }
}
