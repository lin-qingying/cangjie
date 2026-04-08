// RUN_PIPELINE_TILL: FRONTEND
// STUB_FALLBACK_TO_AST:
class TextCommand {}

main(): Int64 {
    let value = try {
        perform TextCommand()
    } handle (cmd: TextCommand) {
        resume with 1
    }
    return value
}
