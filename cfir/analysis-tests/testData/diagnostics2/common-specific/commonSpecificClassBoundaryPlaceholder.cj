// RUN_PIPELINE_TILL: FRONTEND
common open class A {}

specific interface <!CLASSIFIER_REDECLARATION!>A<!> {}
