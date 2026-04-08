// RUN_PIPELINE_TILL: FRONTEND
class Recursive {
    init() {
        <!RECURSIVE_CONSTRUCTOR_CALL!>this<!>()
    }
}
