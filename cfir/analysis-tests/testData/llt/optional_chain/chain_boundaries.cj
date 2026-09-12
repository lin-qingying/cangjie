class Box {
    let value: Int64 = 1
    let nested: ?Int64 = Some(2)
    var writable: Int64 = 0
    func generic<T>(): T { throw Exception() }
    func take(value: Int64): Int64 { value }
}

type OptionalBox = Option<Box>
func acceptSingle(value: ?Int64): Unit {}
func acceptNested(value: ??Int64): Unit {}
func acceptUnit(value: Unit): Unit {}

func resultLayers(value: ?Box): Unit {
    acceptSingle(value?.value)
    acceptNested(value?.nested)
    acceptSingle(<!ARGUMENT_TYPE_MISMATCH!>value?.nested<!>)
}

func aliasReceiver(value: OptionalBox): Unit {
    acceptSingle(value?.value)
}

func expectedResultDoesNotInfer(value: ?Box): Unit {
    let result: ?Int64 = <!UNABLE_TO_INFER_GENERIC_FUNC!>value?.generic()<!>
}

func invalidBaseStopsArguments(value: Box): Unit {
    <!OPTIONAL_CHAIN_NON_OPTIONAL!>value?.take(missingArgument)<!>
    <!OPTIONAL_CHAIN_NON_OPTIONAL!>value?.nested?.missingMember<!>
}

func unknownBaseStopsArguments(): Unit {
    <!UNRESOLVED_REFERENCE!>missingBase<!>?.take(missingArgument)
}

func assignmentResult(value: ?Box): Unit {
    acceptUnit(value?.writable++)
    let result: ?Unit = value?.writable++
}

func nestedArgument(value: ?Box, other: ?Box): Unit {
    acceptSingle(value?.take(other?.value ?? 0))
}

func takeTuple(value: (?Int64, Int64)): Unit {}
func takeNamed(value!: Int64): Unit {}

func tupleArgument(value: ?Box): Unit {
    takeTuple(<!ARGUMENT_TYPE_MISMATCH!>(value?.nested, 0)<!>)
}

func namedArgument(value: ?Box): Unit {
    takeNamed(<!NEED_NAMED_ARGUMENT!>value?.missing()<!>)
    takeNamed(<!NEED_NAMED_ARGUMENT!>value?.missing({ p => missingBody })<!>)
}

func compoundAssignment(value: ?Box, items: ?Array<Int64>): Unit {
    value?.writable += 1
    items?[0] += 1
}
