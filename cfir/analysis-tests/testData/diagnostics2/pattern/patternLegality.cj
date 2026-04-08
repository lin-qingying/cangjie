// RUN_PIPELINE_TILL: FRONTEND
enum Option<T> {
    Some(T) | None
}

func tuplePatternOnInt(v: Int64): Int64 {
    return match (v) {
        case <!TUPLE_PATTERN_NOT_MATCH!>(a, b)<!> => 0
    }
}

func constPatternOnEnum(v: Option<Int64>): Int64 {
    return match (v) {
        case <!NOT_OVERLOAD_IN_MATCH!>1<!> => 0
        case Some(value) => value
        case None => 2
    }
}

func enumPatternWrongArity(v: Option<Int64>): Int64 {
    return match (v) {
        case <!ENUM_PATTERN_PARAM_SIZE_ERROR!>Some()<!> => 0
        case Some(value) => value
        case None => 1
    }
}
