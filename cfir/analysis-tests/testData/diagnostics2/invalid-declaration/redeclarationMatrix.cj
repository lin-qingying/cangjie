// RUN_PIPELINE_TILL: FRONTEND
interface abc {
    func ab(): Unit
    func <!CONFLICTING_OVERLOADS!>ab<!>(): Unit
}

class <!CLASSIFIER_REDECLARATION!>abc<!> {
    let a = 1
    let <!REDECLARATION!>a<!> = 2
}

func top() {}
func <!CONFLICTING_OVERLOADS!>top<!>() {}
