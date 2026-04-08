// RUN_PIPELINE_TILL: FRONTEND
interface MutCarrier {
    <!MUT_ONLY_ON_FUNCTION!>mut func denied(): Unit<!>
}

struct Counter {
    mut func allowed(): Unit {}
}
