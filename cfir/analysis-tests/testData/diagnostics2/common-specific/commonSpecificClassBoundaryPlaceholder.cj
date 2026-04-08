// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
<!COMMON_OPEN_CLASS_NO_INIT!>/* suggested message: common open class declarations require compatible initialization semantics before specific realization. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */common open class A<!>

<!SPECIFIC_HAS_DIFFERENT_TYPE!>/* suggested message: specific declaration must match the common declaration type shape. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md */specific interface A<!>
