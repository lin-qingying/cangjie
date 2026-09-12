func generic<T>(): Unit {}
func noArgs(): Unit {}
func accept(value: Int64): Unit {}

func erroneousValue(): Unit {
    let broken = <!GENERIC_ARGUMENT_NO_MATCH!>noArgs<Int64>()<!>
    broken(generic.foo())
    broken({x => missingBody})
    broken({x: <!UNDECLARED_TYPE_NAME!>MissingInput<!> => missingBody})
}

class Owner {
    public let broken = <!GENERIC_ARGUMENT_NO_MATCH!>noArgs<Int64>()<!>
}

func erroneousMember(): Unit {
    Owner().broken(generic.foo())
}

func erroneousReceiver(): Unit {
    <!UNRESOLVED_REFERENCE!>missingReceiver<!>.member({x => missingBody})
    <!UNRESOLVED_REFERENCE!>missingReceiver<!>.member({x: <!UNDECLARED_TYPE_NAME!>MissingReceiverInput<!> => missingBody})
}

func validCallees(): Unit {
    accept(<!UNRESOLVED_REFERENCE!>missingArgument<!>)
    let valid: (Int64) -> Unit = {x =>}
    valid(<!UNRESOLVED_REFERENCE!>missingValueArgument<!>)
}
