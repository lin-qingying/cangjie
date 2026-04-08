// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
common interface Config {
    func ready(): Bool
}

<!SPECIFIC_HAS_DIFFERENT_TYPE!>/* suggested message: specific declaration kind must match the paired common declaration kind. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */specific struct Config {}<!>
