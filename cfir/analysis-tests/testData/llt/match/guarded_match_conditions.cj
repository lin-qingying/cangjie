func onlyTrueGuard(x: Int64): Int64 {
    match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _ where true => 1
    }
}

func onlyFalseGuard(x: Int64): Int64 {
    match (<!NON_EXHAUSTIVE_MATCH!>x<!>) {
        case _ where false => 1
    }
}

func invalidLiteralGuard(x: Int64): Int64 {
    match (x) {
        case _ where <!CANNOT_CONVERT_LITERAL!>1<!> => 1
        case _ => 2
    }
}

func invalidValueGuard(x: Int64): Int64 {
    match (x) {
        case _ where <!TYPE_MISMATCH!>x<!> => 1
        case _ => 2
    }
}

func invalidLambdaCondition(): Int64 {
    match {
        case <!TYPE_MISMATCH!>{=>}<!> => 1
        case _ => 2
    }
}

func missingDefault(x: Bool): Int64 {
    <!MATCH_CASE_MUST_HAVE_DEFAULT!>match<!> {
        case x => 1
    }
}

func trueCondition(): Int64 {
    <!MATCH_CASE_MUST_HAVE_DEFAULT!>match<!> {
        case true => 1
    }
}

func invalidAction(x: Int64): Int64 {
    match (x) {
        case 1 => <!UNRESOLVED_REFERENCE!>missing<!>
    }
}

func invalidGuardWithoutDefault(x: Int64): Int64 {
    match (x) {
        case _ where <!TYPE_MISMATCH!>x<!> => 1
    }
}

func validGuards(x: Int64, flag: Bool): Int64 {
    match (x) {
        case 1 where flag => 1
        case _ => 2
    }
}
