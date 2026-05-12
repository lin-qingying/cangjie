// RUN_PIPELINE_TILL: FRONTEND
common open class Service {}

specific interface <!CLASSIFIER_REDECLARATION!>Service<!> {}
