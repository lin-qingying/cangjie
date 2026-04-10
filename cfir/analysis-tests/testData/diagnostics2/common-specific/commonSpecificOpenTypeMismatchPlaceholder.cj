// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
<!COMMON_OPEN_CLASS_NO_INIT!>/* suggested message: at least one constructor is required in common class 'Service'. source: DiagnosticParser.def:parse_cjmp_in_common_ctor_required; ParseCJMPDecl.cpp:CheckCJMPCtorPresence */common open class Service<!>

<!SPECIFIC_HAS_DIFFERENT_TYPE!>/* suggested message: 'specific' decl kind(interface) is not equal to 'common'(class). source: DiagnosticSema.def:specific_has_different_kind; CheckCJMP.cpp:MatchNominativeDecl */specific interface Service {}<!>
