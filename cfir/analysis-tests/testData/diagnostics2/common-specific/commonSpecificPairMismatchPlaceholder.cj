// RUN_PIPELINE_TILL: FRONTEND
common interface Printable {
    func print(): Unit
}

specific interface <!CLASSIFIER_REDECLARATION!>Printable<!> {
    func print(): Unit
}

specific class <!CLASSIFIER_REDECLARATION!>Printable<!> {}
