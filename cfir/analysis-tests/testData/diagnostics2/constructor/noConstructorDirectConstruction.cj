// RUN_PIPELINE_TILL: FRONTEND
interface Factory {}

func directConstruction(): Unit {
    let _ = <!NO_CONSTRUCTOR!>Factory<!>()
}
