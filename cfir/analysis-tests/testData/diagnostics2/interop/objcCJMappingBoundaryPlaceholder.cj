// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@ObjCImpl
interface BadMapping {}

<!OBJC_CJ_MAPPING_MUST_BE_CLASS!>/* suggested message: Objective-C CJMapping declarations must satisfy dedicated mapping type restrictions and cannot use unsupported declaration kinds. source: external/cangjie_compiler/src/Sema/NativeFFI/ObjC/AfterTypeCheck/Interop/CheckCJMappingTypes.cpp */@ObjCImpl
interface AnotherBadMapping {}<!>
