// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
common open class Service

<!SPECIFIC_HAS_DIFFERENT_TYPE!>/* suggested message: specific realization of a common open class must preserve the declaration kind. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */specific interface Service {}<!>
