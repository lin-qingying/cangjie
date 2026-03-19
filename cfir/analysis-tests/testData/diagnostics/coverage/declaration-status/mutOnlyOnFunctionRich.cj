// RUN_PIPELINE_TILL: FRONTEND
<!MUT_ONLY_ON_FUNCTION!>mut class MutClass {}<!>

class MutHolder {
    <!MUT_ONLY_ON_FUNCTION!>mut var value: Int64 = 0<!>
    <!MUT_ONLY_ON_FUNCTION!>mut let fixed: Int64 = 1<!>
    mut func allowed(): Unit {}
}

<!MUT_ONLY_ON_FUNCTION!>mut struct MutStruct {}<!>

<!MUT_ONLY_ON_FUNCTION!>mut enum MutEnum {
    | A
}<!>

