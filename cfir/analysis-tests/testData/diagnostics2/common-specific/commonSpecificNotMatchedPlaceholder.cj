// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
common interface Printable {
    func print(): Unit
}

<!NOT_MATCHED!>/* suggested message: specific declaration does not satisfy the matching common declaration contract. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */specific class Printable {}<!>
