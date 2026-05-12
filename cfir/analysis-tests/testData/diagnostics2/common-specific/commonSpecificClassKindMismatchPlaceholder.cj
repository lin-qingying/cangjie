// RUN_PIPELINE_TILL: FRONTEND
common class Repo {}

specific interface <!CLASSIFIER_REDECLARATION!>Repo<!> {}
