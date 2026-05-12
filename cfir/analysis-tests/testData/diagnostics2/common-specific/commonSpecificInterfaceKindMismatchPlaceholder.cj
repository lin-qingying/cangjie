// RUN_PIPELINE_TILL: FRONTEND
common interface Config {
    func ready(): Bool
}

specific struct <!CLASSIFIER_REDECLARATION!>Config<!> {}
