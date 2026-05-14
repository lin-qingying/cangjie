// RUN_PIPELINE_TILL: FRONTEND
interface abc {
    func ab(): Unit
    <!CONFLICTING_OVERLOADS!>func ab()<!>: Unit
}

class <!CLASSIFIER_REDECLARATION!>abc<!> {
    let a = 1
    let <!REDECLARATION!>a<!> = 2
}

func top() {}
<!CONFLICTING_OVERLOADS!>func top()<!> {}
