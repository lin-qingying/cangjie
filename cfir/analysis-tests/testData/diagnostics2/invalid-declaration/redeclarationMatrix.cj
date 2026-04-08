// RUN_PIPELINE_TILL: FRONTEND
interface <!CLASSIFIER_REDECLARATION!>abc<!> {
    <!CONFLICTING_OVERLOADS!>func ab()<!>: Unit
    <!CONFLICTING_OVERLOADS!>func ab()<!>: Unit
}

class <!CLASSIFIER_REDECLARATION!>abc<!> {
    let <!REDECLARATION!>a<!> = 1
    let <!REDECLARATION!>a<!> = 2
}

<!CONFLICTING_OVERLOADS!>func top()<!> {}
<!CONFLICTING_OVERLOADS!>func top()<!> {}
