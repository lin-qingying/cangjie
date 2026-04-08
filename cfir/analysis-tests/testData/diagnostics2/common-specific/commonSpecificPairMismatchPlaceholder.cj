// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
common interface Printable {
    func print(): Unit
}

specific interface Printable {
    func print(): Unit
}

<!SPECIFIC_HAS_DIFFERENT_TYPE!>/* suggested message: paired common/specific declarations must agree on declaration kind and matching contract. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */specific class Printable {}<!>
