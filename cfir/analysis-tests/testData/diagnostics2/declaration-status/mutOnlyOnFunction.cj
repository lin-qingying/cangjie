// RUN_PIPELINE_TILL: FRONTEND
interface MutCarrier {
    mut func allowed(): Unit
}

struct Counter {
    mut func allowed(): Unit {}
}
