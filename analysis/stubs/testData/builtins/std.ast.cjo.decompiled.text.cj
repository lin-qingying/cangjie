// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.ast

import std.core.*
import std.collection.ArrayList

let NUM_SPACES: UInt16 = COMPILED_CODE /* compiled code */

internal enum NodeFormat_AnyExpr {
    | AnyExpr_NONE

    | AnyExpr_BINARY_EXPR

    | AnyExpr_UNARY_EXPR

    | AnyExpr_PAREN_EXPR

    | AnyExpr_LIT_CONST_EXPR

    | AnyExpr_CALL_EXPR

    | AnyExpr_REF_EXPR

    | AnyExpr_RETURN_EXPR

    | AnyExpr_ASSIGN_EXPR

    | AnyExpr_MEMBER_ACCESS

    | AnyExpr_IF_EXPR

    | AnyExpr_BLOCK

    | AnyExpr_LAMBDA_EXPR

    | AnyExpr_ARRAY_LIT

    | AnyExpr_TUPLE_LIT

    | AnyExpr_SUBSCRIPT_EXPR

    | AnyExpr_RANGE_EXPR

    | AnyExpr_FOR_IN_EXPR

    | AnyExpr_IS_EXPR

    | AnyExpr_AS_EXPR

    | AnyExpr_TRAILING_CLOSURE_EXPR

    | AnyExpr_TYPE_CONV_EXPR

    | AnyExpr_THROW_EXPR

    | AnyExpr_TRY_EXPR

    | AnyExpr_PRIMITIVE_TYPE_EXPR

    | AnyExpr_WHILE_EXPR

    | AnyExpr_DO_WHILE_EXPR

    | AnyExpr_JUMP_EXPR

    | AnyExpr_INC_OR_DEC_EXPR

    | AnyExpr_SPAWN_EXPR

    | AnyExpr_SYNCHRONIZED_EXPR

    | AnyExpr_LET_PATTERN_DESTRUCTOR

    | AnyExpr_TOKEN_PART

    | AnyExpr_QUOTE_EXPR

    | AnyExpr_MATCH_EXPR

    | AnyExpr_OPTIONAL_EXPR

    | AnyExpr_OPTIONAL_CHAIN_EXPR

    | AnyExpr_MACRO_EXPAND_EXPR

    | AnyExpr_WILDCARD_EXPR

    | AnyExpr_ARRAY_EXPR
}

internal func EnumValuesNodeFormat_AnyExpr(e: UInt8): std.ast.NodeFormat_AnyExpr { /* compiled code */ }

internal enum NodeFormat_AnyPattern {
    | AnyPattern_NONE

    | AnyPattern_CONST_PATTERN

    | AnyPattern_WILDCARD_PATTERN

    | AnyPattern_VAR_PATTERN

    | AnyPattern_EXCEPT_TYPE_PATTERN

    | AnyPattern_TYPE_PATTERN

    | AnyPattern_ENUM_PATTERN

    | AnyPattern_VAR_OR_ENUM_PATTERN

    | AnyPattern_TUPLE_PATTERN

    | AnyPattern_COMMAND_TYPE_PATTERN
}

internal func EnumValuesNodeFormat_AnyPattern(e: UInt8): std.ast.NodeFormat_AnyPattern { /* compiled code */ }

internal enum NodeFormat_MacroParam {
    | MacroParam_NONE

    | MacroParam_MACRO_EXPAND_PARAM
}

internal func EnumValuesNodeFormat_MacroParam(e: UInt8): std.ast.NodeFormat_MacroParam { /* compiled code */ }

internal enum NodeFormat_AnyType {
    | AnyType_NONE

    | AnyType_REF_TYPE

    | AnyType_PRIMITIVE_TYPE

    | AnyType_FUNC_TYPE

    | AnyType_THIS_TYPE

    | AnyType_PAREN_TYPE

    | AnyType_QUALIFIED_TYPE

    | AnyType_OPTION_TYPE

    | AnyType_TUPLE_TYPE

    | AnyType_VARRAY_TYPE

    | AnyType_CONSTANT_TYPE
}

internal func EnumValuesNodeFormat_AnyType(e: UInt8): std.ast.NodeFormat_AnyType { /* compiled code */ }

internal enum NodeFormat_AnyDecl {
    | AnyDecl_NONE

    | AnyDecl_FUNC_DECL

    | AnyDecl_MAIN_DECL

    | AnyDecl_VAR_DECL

    | AnyDecl_VAR_WITH_PATTERN_DECL

    | AnyDecl_STRUCT_DECL

    | AnyDecl_CLASS_DECL

    | AnyDecl_INTERFACE_DECL

    | AnyDecl_GENERIC_PARAM_DECL

    | AnyDecl_PRIMARY_CTOR_DECL

    | AnyDecl_PROP_DECL

    | AnyDecl_ENUM_DECL

    | AnyDecl_TYPE_ALIAS_DECL

    | AnyDecl_EXTEND_DECL

    | AnyDecl_MACRO_DECL

    | AnyDecl_MACRO_EXPAND_DECL

    | AnyDecl_FUNC_PARAM
}

internal func EnumValuesNodeFormat_AnyDecl(e: UInt8): std.ast.NodeFormat_AnyDecl { /* compiled code */ }

internal enum NodeFormat_AnyNode {
    | AnyNode_NONE

    | AnyNode_EXPR

    | AnyNode_DECL

    | AnyNode_TYPE

    | AnyNode_PATTERN

    | AnyNode_FILE

    | AnyNode_PACKAGE_SPEC

    | AnyNode_IMPORT_SPEC

    | AnyNode_ANNOTATION
}

internal func EnumValuesNodeFormat_AnyNode(e: UInt8): std.ast.NodeFormat_AnyNode { /* compiled code */ }

internal enum NodeFormat_AccessibleKind {
    | AccessibleKind_ACCESSIBLE_PUBLIC

    | AccessibleKind_ACCESSIBLE_PROTECTED

    | AccessibleKind_ACCESSIBLE_INTERNAL
}

internal func EnumValuesNodeFormat_AccessibleKind(e: UInt8): std.ast.NodeFormat_AccessibleKind { /* compiled code */ }

internal enum NodeFormat_ImportKind {
    | ImportKind_IMPORT_SINGLE

    | ImportKind_IMPORT_ALIAS

    | ImportKind_IMPORT_ALL

    | ImportKind_IMPORT_MULTI
}

internal func EnumValuesNodeFormat_ImportKind(e: UInt8): std.ast.NodeFormat_ImportKind { /* compiled code */ }

internal enum NodeFormat_ReExportKind {
    | ReExportKind_REEXPORT_PRIVATE

    | ReExportKind_REEXPORT_INTERNAL

    | ReExportKind_REEXPORT_PROTECTED

    | ReExportKind_REEXPORT_PUBLIC
}

internal func EnumValuesNodeFormat_ReExportKind(e: UInt8): std.ast.NodeFormat_ReExportKind { /* compiled code */ }

internal struct NodeFormat_Position {
    internal static let BYTE_ALIGNMENT: UInt32

    internal let fileId: UInt32

    internal let line: Int32

    internal let column: Int32

    init() { /* compiled code */ }

    init(buf: std.core.Array<UInt8>, pos: UInt32) { /* compiled code */ }
}

public class NodeFormat_MatchCase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERNS: UInt16

    internal let PATTERNGUARD: UInt16

    internal let IF_POS: UInt16

    internal let ARROW_POS: UInt16

    internal let EXPR_OR_DECLS: UInt16

    internal let BIT_OR_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    internal func GetPatternguard(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExprOrDecls(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_NodeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BEGIN: UInt16

    internal let END: UInt16

    internal let AST_KIND: UInt16

    internal func GetBegin(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetEnd(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAstKind(): std.core.String { /* compiled code */ }
}

public class NodeFormat_Pattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERN_TYPE: UInt16

    internal let PATTERN: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPatternType(): std.ast.NodeFormat_AnyPattern { /* compiled code */ }

    internal func GetPatternAsConstPattern(): std.core.Option<std.ast.NodeFormat_ConstPattern> { /* compiled code */ }

    internal func GetPatternAsWildcardPattern(): std.core.Option<std.ast.NodeFormat_WildcardPattern> { /* compiled code */ }

    internal func GetPatternAsVarPattern(): std.core.Option<std.ast.NodeFormat_VarPattern> { /* compiled code */ }

    internal func GetPatternAsExceptTypePattern(): std.core.Option<std.ast.NodeFormat_ExceptTypePattern> { /* compiled code */ }

    internal func GetPatternAsTypePattern(): std.core.Option<std.ast.NodeFormat_TypePattern> { /* compiled code */ }

    internal func GetPatternAsEnumPattern(): std.core.Option<std.ast.NodeFormat_EnumPattern> { /* compiled code */ }

    internal func GetPatternAsVarOrEnumPattern(): std.core.Option<std.ast.NodeFormat_VarOrEnumPattern> { /* compiled code */ }

    internal func GetPatternAsTuplePattern(): std.core.Option<std.ast.NodeFormat_TuplePattern> { /* compiled code */ }

    internal func GetPatternAsCommandTypePattern(): std.core.Option<std.ast.NodeFormat_CommandTypePattern> { /* compiled code */ }
}

public class NodeFormat_Expr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR_TYPE: UInt16

    internal let EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExprType(): std.ast.NodeFormat_AnyExpr { /* compiled code */ }

    internal func GetExprAsBinaryExpr(): std.core.Option<std.ast.NodeFormat_BinaryExpr> { /* compiled code */ }

    internal func GetExprAsUnaryExpr(): std.core.Option<std.ast.NodeFormat_UnaryExpr> { /* compiled code */ }

    internal func GetExprAsParenExpr(): std.core.Option<std.ast.NodeFormat_ParenExpr> { /* compiled code */ }

    internal func GetExprAsLitConstExpr(): std.core.Option<std.ast.NodeFormat_LitConstExpr> { /* compiled code */ }

    internal func GetExprAsCallExpr(): std.core.Option<std.ast.NodeFormat_CallExpr> { /* compiled code */ }

    internal func GetExprAsRefExpr(): std.core.Option<std.ast.NodeFormat_RefExpr> { /* compiled code */ }

    internal func GetExprAsReturnExpr(): std.core.Option<std.ast.NodeFormat_ReturnExpr> { /* compiled code */ }

    internal func GetExprAsAssignExpr(): std.core.Option<std.ast.NodeFormat_AssignExpr> { /* compiled code */ }

    internal func GetExprAsMemberAccess(): std.core.Option<std.ast.NodeFormat_MemberAccess> { /* compiled code */ }

    internal func GetExprAsIfExpr(): std.core.Option<std.ast.NodeFormat_IfExpr> { /* compiled code */ }

    internal func GetExprAsBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetExprAsLambdaExpr(): std.core.Option<std.ast.NodeFormat_LambdaExpr> { /* compiled code */ }

    internal func GetExprAsArrayLit(): std.core.Option<std.ast.NodeFormat_ArrayLit> { /* compiled code */ }

    internal func GetExprAsTupleLit(): std.core.Option<std.ast.NodeFormat_TupleLit> { /* compiled code */ }

    internal func GetExprAsSubscriptExpr(): std.core.Option<std.ast.NodeFormat_SubscriptExpr> { /* compiled code */ }

    internal func GetExprAsRangeExpr(): std.core.Option<std.ast.NodeFormat_RangeExpr> { /* compiled code */ }

    internal func GetExprAsForInExpr(): std.core.Option<std.ast.NodeFormat_ForInExpr> { /* compiled code */ }

    internal func GetExprAsIsExpr(): std.core.Option<std.ast.NodeFormat_IsExpr> { /* compiled code */ }

    internal func GetExprAsAsExpr(): std.core.Option<std.ast.NodeFormat_AsExpr> { /* compiled code */ }

    internal func GetExprAsTrailingClosureExpr(): std.core.Option<std.ast.NodeFormat_TrailingClosureExpr> { /* compiled code */ }

    internal func GetExprAsTypeConvExpr(): std.core.Option<std.ast.NodeFormat_TypeConvExpr> { /* compiled code */ }

    internal func GetExprAsThrowExpr(): std.core.Option<std.ast.NodeFormat_ThrowExpr> { /* compiled code */ }

    internal func GetExprAsTryExpr(): std.core.Option<std.ast.NodeFormat_TryExpr> { /* compiled code */ }

    internal func GetExprAsPrimitiveTypeExpr(): std.core.Option<std.ast.NodeFormat_PrimitiveTypeExpr> { /* compiled code */ }

    internal func GetExprAsWhileExpr(): std.core.Option<std.ast.NodeFormat_WhileExpr> { /* compiled code */ }

    internal func GetExprAsDoWhileExpr(): std.core.Option<std.ast.NodeFormat_DoWhileExpr> { /* compiled code */ }

    internal func GetExprAsJumpExpr(): std.core.Option<std.ast.NodeFormat_JumpExpr> { /* compiled code */ }

    internal func GetExprAsIncOrDecExpr(): std.core.Option<std.ast.NodeFormat_IncOrDecExpr> { /* compiled code */ }

    internal func GetExprAsSpawnExpr(): std.core.Option<std.ast.NodeFormat_SpawnExpr> { /* compiled code */ }

    internal func GetExprAsSynchronizedExpr(): std.core.Option<std.ast.NodeFormat_SynchronizedExpr> { /* compiled code */ }

    internal func GetExprAsLetPatternDestructor(): std.core.Option<std.ast.NodeFormat_LetPatternDestructor> { /* compiled code */ }

    internal func GetExprAsTokenPart(): std.core.Option<std.ast.NodeFormat_TokenPart> { /* compiled code */ }

    internal func GetExprAsQuoteExpr(): std.core.Option<std.ast.NodeFormat_QuoteExpr> { /* compiled code */ }

    internal func GetExprAsMatchExpr(): std.core.Option<std.ast.NodeFormat_MatchExpr> { /* compiled code */ }

    internal func GetExprAsOptionalExpr(): std.core.Option<std.ast.NodeFormat_OptionalExpr> { /* compiled code */ }

    internal func GetExprAsOptionalChainExpr(): std.core.Option<std.ast.NodeFormat_OptionalChainExpr> { /* compiled code */ }

    internal func GetExprAsMacroExpandExpr(): std.core.Option<std.ast.NodeFormat_MacroExpandExpr> { /* compiled code */ }

    internal func GetExprAsWildcardExpr(): std.core.Option<std.ast.NodeFormat_WildcardExpr> { /* compiled code */ }

    internal func GetExprAsArrayExpr(): std.core.Option<std.ast.NodeFormat_ArrayExpr> { /* compiled code */ }
}

public class NodeFormat_Block <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let BODY: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal let IS_UNSAFE: UInt16

    internal let UNSAFE_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetBody(): std.core.Array<std.core.Option<std.ast.NodeFormat_Node>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsUnsafe(): Bool { /* compiled code */ }

    internal func GetUnsafePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public open class FlatBufferObject {
    internal let table: std.ast.Table

    init(buf: std.core.Array<UInt8>, root: UInt32) { /* compiled code */ }
}

public class NodeFormat_MatchCaseOther <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let MATCH_EXPR: UInt16

    internal let ARROW_POS: UInt16

    internal let EXPR_OR_DECLS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetMatchExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExprOrDecls(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

public class NodeFormat_ConstPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LITERAL: UInt16

    internal let OPERATOR_CALL_EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLiteral(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetOperatorCallExpr(): std.core.Option<std.ast.NodeFormat_CallExpr> { /* compiled code */ }
}

public class NodeFormat_WildcardPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }
}

public class NodeFormat_VarPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let VAR_DECL: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetVarDecl(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }
}

public class NodeFormat_ExceptTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERN: UInt16

    internal let PATTERN_POS: UInt16

    internal let COLON_POS: UInt16

    internal let TYPES: UInt16

    internal let BIT_OR_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetPatternPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_TypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERN: UInt16

    internal let COLON_POS: UInt16

    internal let TYPE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }
}

public class NodeFormat_EnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let REF: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let PATTERNS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let COMMA_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetRef(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_VarOrEnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let IDENTIFIER: UInt16

    internal let PATTERN: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }
}

public class NodeFormat_TuplePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let PATTERNS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let COMMA_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_CommandTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERN: UInt16

    internal let PATTERN_POS: UInt16

    internal let COLON_POS: UInt16

    internal let TYPES: UInt16

    internal let BIT_OR_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetPatternPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_CallExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BASE_FUNC: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let ARGS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBaseFunc(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_VarDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE: UInt16

    internal let COLON_POS: UInt16

    internal let INITIALIZER: UInt16

    internal let ASSIGN_POS: UInt16

    internal let IS_VAR: UInt16

    internal let ISENUMCONSTRUCT: UInt16

    internal let EMPTYKEYWORD: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsVar(): Bool { /* compiled code */ }

    internal func GetIsEnumConstruct(): Bool { /* compiled code */ }

    internal func GetEmptyKeyword(): Bool { /* compiled code */ }
}

public class NodeFormat_Type <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE_TYPE: UInt16

    internal let TYPE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetTypeType(): std.ast.NodeFormat_AnyType { /* compiled code */ }

    internal func GetTypeAsRefType(): std.core.Option<std.ast.NodeFormat_RefType> { /* compiled code */ }

    internal func GetTypeAsPrimitiveType(): std.core.Option<std.ast.NodeFormat_PrimitiveType> { /* compiled code */ }

    internal func GetTypeAsFuncType(): std.core.Option<std.ast.NodeFormat_FuncType> { /* compiled code */ }

    internal func GetTypeAsThisType(): std.core.Option<std.ast.NodeFormat_ThisType> { /* compiled code */ }

    internal func GetTypeAsParenType(): std.core.Option<std.ast.NodeFormat_ParenType> { /* compiled code */ }

    internal func GetTypeAsQualifiedType(): std.core.Option<std.ast.NodeFormat_QualifiedType> { /* compiled code */ }

    internal func GetTypeAsOptionType(): std.core.Option<std.ast.NodeFormat_OptionType> { /* compiled code */ }

    internal func GetTypeAsTupleType(): std.core.Option<std.ast.NodeFormat_TupleType> { /* compiled code */ }

    internal func GetTypeAsVArrayType(): std.core.Option<std.ast.NodeFormat_VArrayType> { /* compiled code */ }

    internal func GetTypeAsConstantType(): std.core.Option<std.ast.NodeFormat_ConstantType> { /* compiled code */ }
}

public class NodeFormat_BinaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_EXPR: UInt16

    internal let RIGHT_EXPR: UInt16

    internal let OPERATOR_KIND: UInt16

    internal let OPERATOR_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetOperatorKind(): UInt16 { /* compiled code */ }

    internal func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_IsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR: UInt16

    internal let IS_TYPE: UInt16

    internal let IS_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetIsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetIsPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_AsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR: UInt16

    internal let AS_TYPE: UInt16

    internal let AS_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetAsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetAsPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_UnaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR: UInt16

    internal let OPERATOR_KIND: UInt16

    internal let OPERATOR_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetOperatorKind(): UInt16 { /* compiled code */ }

    internal func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_WildcardExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }
}

public class NodeFormat_ArrayExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE: UInt16

    internal let LEFTPARENPOS: UInt16

    internal let ARGS: UInt16

    internal let RIGHTPARENPOS: UInt16

    internal let ISVALUEARRAY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsValueArray(): Bool { /* compiled code */ }
}

public class NodeFormat_FuncArg <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let NAME: UInt16

    internal let NAME_POS: UInt16

    internal let COLON_POS: UInt16

    internal let EXPR: UInt16

    internal let COMMA_POS: UInt16

    internal let WITHINOUT: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetName(): std.core.String { /* compiled code */ }

    internal func GetNamePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetWithInout(): Bool { /* compiled code */ }
}

public class NodeFormat_ParenExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_LitConstExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LITERAL: UInt16

    internal let LITERAL_CONST_KIND: UInt16

    internal let DELIMITER_NUM: UInt16

    internal let STRING_KIND: UInt16

    internal let IS_SINGLE_QUOTE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLiteral(): std.core.String { /* compiled code */ }

    internal func GetLiteralConstKind(): UInt16 { /* compiled code */ }

    internal func GetDelimiterNum(): UInt16 { /* compiled code */ }

    internal func GetStringKind(): UInt16 { /* compiled code */ }

    internal func GetIsSingleQuote(): Bool { /* compiled code */ }
}

public class NodeFormat_RefExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let REF: UInt16

    internal let LEFT_ANGLE_POS: UInt16

    internal let TYPE_ARGUMENTS: UInt16

    internal let RIGHT_ANGLE_POS: UInt16

    internal let IS_THIS: UInt16

    internal let IS_SUPER: UInt16

    internal let IS_QUOTE_DOLLAR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetRef(): std.core.Option<std.ast.NodeFormat_Reference> { /* compiled code */ }

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsThis(): Bool { /* compiled code */ }

    internal func GetIsSuper(): Bool { /* compiled code */ }

    internal func GetIsQuoteDollar(): Bool { /* compiled code */ }
}

public class NodeFormat_Reference <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let IDENTIFIER: UInt16

    internal let IDENTIFIER_POS: UInt16

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_ReturnExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let RETURN_POS: UInt16

    internal let EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetReturnPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_AssignExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_VALUE: UInt16

    internal let ASSIGN_OP: UInt16

    internal let ASSIGN_POS: UInt16

    internal let RIGHT_EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftValue(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetAssignOp(): UInt16 { /* compiled code */ }

    internal func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_MemberAccess <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BASE_EXPR: UInt16

    internal let DOT_POS: UInt16

    internal let FIELD: UInt16

    internal let FIELD_POS: UInt16

    internal let LEFT_ANGLE_POS: UInt16

    internal let TYPE_ARGUMENTS: UInt16

    internal let RIGHT_ANGLE_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetDotPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetField(): std.core.String { /* compiled code */ }

    internal func GetFieldPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_IfExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let IF_POS: UInt16

    internal let COND_EXPR: UInt16

    internal let BODY: UInt16

    internal let HAS_ELSE: UInt16

    internal let ELSE_POS: UInt16

    internal let ELSE_BODY: UInt16

    internal let ISELSEIF: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetHasElse(): Bool { /* compiled code */ }

    internal func GetElsePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetElseBody(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetIsElseIf(): Bool { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_LetPatternDestructor <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PATTERNS: UInt16

    internal let BIT_OR_POS_VEC: UInt16

    internal let BACKARROW_POS: UInt16

    internal let INITIALIZER: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    internal func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetBackarrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_LambdaExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BODY: UInt16

    internal let MOCKSUPPORTED: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }

    internal func GetMockSupported(): Bool { /* compiled code */ }
}

public class NodeFormat_FuncBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let PARAM_LIST: UInt16

    internal let ARROW_POS: UInt16

    internal let COLON_POS: UInt16

    internal let RET_TYPE: UInt16

    internal let HAS_BODY: UInt16

    internal let BODY: UInt16

    internal let GENERIC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetParamList(): std.core.Option<std.ast.NodeFormat_FuncParamList> { /* compiled code */ }

    internal func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetHasBody(): Bool { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

public class NodeFormat_ArrayLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let CHILDREN: UInt16

    internal let COMMA_POS_VEC: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetChildren(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    internal func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_TupleLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let CHILDREN: UInt16

    internal let COMMA_POS_VEC: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetChildren(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    internal func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_SubscriptExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BASE_EXPR: UInt16

    internal let LEFT_SQUARE_POS: UInt16

    internal let INDEX_EXPRS: UInt16

    internal let RIGHT_SQUARE_POS: UInt16

    internal let IS_TUPLE_ACCESS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIndexExprs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    internal func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsTupleAccess(): Bool { /* compiled code */ }
}

public class NodeFormat_MatchExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let MATCH_MODE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let SELECTOR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let MATCH_CASES: UInt16

    internal let MATCH_CASE_OTHERS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetMatchMode(): Bool { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetSelector(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetMatchCases(): std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCase>> { /* compiled code */ }

    internal func GetMatchCaseOthers(): std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCaseOther>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_RangeExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let START_EXPR: UInt16

    internal let RANGE_POS: UInt16

    internal let STOP_EXPR: UInt16

    internal let COLON_POS: UInt16

    internal let STEP_EXPR: UInt16

    internal let IS_CLOSED: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetStartExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRangePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetStopExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetStepExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetIsClosed(): Bool { /* compiled code */ }
}

public class NodeFormat_ForInExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let PATTERN: UInt16

    internal let IN_POS: UInt16

    internal let IN_EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let IF_POS: UInt16

    internal let PATTERN_GUARD: UInt16

    internal let BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetInPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetInExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPatternGuard(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

public class NodeFormat_WhileExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let WHILE_POS: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let COND_EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetWhilePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

public class NodeFormat_SpawnExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let SPAWN_POS: UInt16

    internal let TASK_EXPR: UInt16

    internal let HAS_ARG: UInt16

    internal let SPAWN_ARG_EXPR: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetSpawnPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTaskExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetHasArg(): Bool { /* compiled code */ }

    internal func GetSpawnArgExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_SynchronizedExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let SYNC_POS: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let MUTEX_EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetSyncPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetMutexExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

public class NodeFormat_TrailingClosureExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_LAMBDA: UInt16

    internal let EXPR: UInt16

    internal let LAMBDA: UInt16

    internal let RIGHT_LAMBDA: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftLambda(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLambda(): std.core.Option<std.ast.NodeFormat_LambdaExpr> { /* compiled code */ }

    internal func GetRightLambda(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_TypeConvExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_ThrowExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_TryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let RESOURCE_SPEC: UInt16

    internal let IS_RESOURCE_SPEC: UInt16

    internal let TRY_BLOCK: UInt16

    internal let CATCH_BLOCKS: UInt16

    internal let CATCH_PATTERNS: UInt16

    internal let FINALLY_POS: UInt16

    internal let FINALLY_BLOCK: UInt16

    internal let RESOURCE_SPEC_LPAREN_POS: UInt16

    internal let RESOURCE_SPEC_RPAREN_POS: UInt16

    internal let RESOURCE_SPEC_COMMA_POS_VEC: UInt16

    internal let CATCH_POS_VEC: UInt16

    internal let CATCH_LEFT_PAREN_POS_VEC: UInt16

    internal let CATCH_RIGHT_PAREN_POS_VEC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetResourceSpec(): std.core.Array<std.core.Option<std.ast.NodeFormat_VarDecl>> { /* compiled code */ }

    internal func GetIsResourceSpec(): Bool { /* compiled code */ }

    internal func GetTryBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetCatchBlocks(): std.core.Array<std.core.Option<std.ast.NodeFormat_Block>> { /* compiled code */ }

    internal func GetCatchPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    internal func GetFinallyPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetFinallyBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetResourceSpecLparenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetResourceSpecRparenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetResourceSpecCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetCatchPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetCatchLeftParenPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetCatchRightParenPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_DoWhileExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let DO_POS: UInt16

    internal let BODY: UInt16

    internal let WHILE_POS: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let COND_EXPR: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetDoPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    internal func GetWhilePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_IncOrDecExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let OPERATOR_KIND: UInt16

    internal let OPERATOR_POS: UInt16

    internal let EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetOperatorKind(): UInt16 { /* compiled code */ }

    internal func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_OptionalExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BASEEXPR: UInt16

    internal let QUEST_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetQuestPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_OptionalChainExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXPR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

public class NodeFormat_Token <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let KIND: UInt16

    internal let VALUE: UInt16

    internal let POS: UInt16

    internal func GetKind(): UInt16 { /* compiled code */ }

    internal func GetValue(): std.core.String { /* compiled code */ }

    internal func GetPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_TokenPart <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let TOKENS: UInt16

    internal func GetTokens(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }
}

public class NodeFormat_QuoteExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let EXPRS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetExprs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_FuncParamList <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let PARAMS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetParams(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncParam>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_FuncParam <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let NODEBASE: UInt16

    internal let BASE: UInt16

    internal let COLON_POS: UInt16

    internal let ASSIGNMENT: UInt16

    internal let COMMA_POS: UInt16

    internal let IS_NAMED_PARAM: UInt16

    internal let IS_MEMBER_PARAM: UInt16

    internal let NOT_MARK_POS: UInt16

    internal let HAS_LET_OR_VAR: UInt16

    internal let MACROPARAM_TYPE: UInt16

    internal let MACROPARAM: UInt16

    internal func GetNodeBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAssignment(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsNamedParam(): Bool { /* compiled code */ }

    internal func GetIsMemberParam(): Bool { /* compiled code */ }

    internal func GetNotMarkPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetHasLetOrVar(): Bool { /* compiled code */ }

    internal func GetMacroParamType(): std.ast.NodeFormat_MacroParam { /* compiled code */ }

    internal func GetMacroParamAsMacroExpandParam(): std.core.Option<std.ast.NodeFormat_MacroExpandParam> { /* compiled code */ }
}

public class NodeFormat_MacroExpandParam <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let INVOCATION: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_FuncParam> { /* compiled code */ }

    internal func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }
}

public class NodeFormat_Generic <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let LEFT_ANGLE_POS: UInt16

    internal let TYPE_PARAMETERS: UInt16

    internal let RIGHT_ANGLE_POS: UInt16

    internal let GENERIC_CONSTRAINTS: UInt16

    internal let CONTENT: UInt16

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeParameters(): std.core.Array<std.core.Option<std.ast.NodeFormat_GenericParamDecl>> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetGenericConstraints(): std.core.Array<std.core.Option<std.ast.NodeFormat_GenericConstraint>> { /* compiled code */ }

    internal func GetContent(): std.core.String { /* compiled code */ }
}

public class NodeFormat_JumpExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let IS_BREAK: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetIsBreak(): Bool { /* compiled code */ }
}

public class NodeFormat_PrimitiveTypeExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let KIND: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetKind(): UInt16 { /* compiled code */ }
}

public class NodeFormat_MacroExpandExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let INVOCATION: UInt16

    internal let IDENTIFIER: UInt16

    internal let IDENTIFIERPOS: UInt16

    internal let ANNOTATIONS: UInt16

    internal let MODIFIERS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAnnotations(): std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>> { /* compiled code */ }

    internal func GetModifiers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>> { /* compiled code */ }
}

public class NodeFormat_TypeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let COMMA_POS: UInt16

    internal let TYPE_PARAMETER_NAME: UInt16

    internal let COLON_POS: UInt16

    internal let TYPE_POS: UInt16

    internal let BIT_AND_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeParameterName(): std.core.String { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetBitAndPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_RefType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let REF: UInt16

    internal let LEFT_ANGLE_POS: UInt16

    internal let TYPE_ARGUMENTS: UInt16

    internal let RIGHT_ANGLE_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetRef(): std.core.Option<std.ast.NodeFormat_Reference> { /* compiled code */ }

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_PrimitiveType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE_STR: UInt16

    internal let KIND: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetTypeStr(): std.core.String { /* compiled code */ }

    internal func GetKind(): UInt16 { /* compiled code */ }
}

public class NodeFormat_FuncType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let PARAM_TYPES: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let ARROW_POS: UInt16

    internal let RET_TYPE: UInt16

    internal let ISC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetParamTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetIsC(): Bool { /* compiled code */ }
}

public class NodeFormat_ThisType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }
}

public class NodeFormat_ParenType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let TYPE: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_QualifiedType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BASE_TYPE: UInt16

    internal let DOT_POS: UInt16

    internal let FIELD: UInt16

    internal let FIELD_POS: UInt16

    internal let LEFT_ANGLE_POS: UInt16

    internal let TYPE_ARGUMENTS: UInt16

    internal let RIGHT_ANGLE_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetBaseType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetDotPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetField(): std.core.String { /* compiled code */ }

    internal func GetFieldPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_OptionType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let COMPONENT_TYPE: UInt16

    internal let QUEST_NUM: UInt16

    internal let QUEST_VECTOR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetComponentType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetQuestNum(): Int32 { /* compiled code */ }

    internal func GetQuestVector(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_TupleType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let FIELD_TYPES: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let COMMA_POS_VECTOR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetFieldTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetCommaPosVector(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

public class NodeFormat_VArrayType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let VARRAYPOS: UInt16

    internal let LEFTANGLEPOS: UInt16

    internal let TYPEARGUMENT: UInt16

    internal let CONSTANTTYPE: UInt16

    internal let RIGHTANGLEPOS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetVarrayPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetTypeArgument(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetConstantType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_ConstantType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let CONSTANTEXPR: UInt16

    internal let DOLLARPOS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    internal func GetConstantExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetDollarPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_DeclBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let IDENTIFIER: UInt16

    internal let IDENTIFIER_POS: UInt16

    internal let KEYWORD_POS: UInt16

    internal let ANNOTATIONS: UInt16

    internal let MODIFIERS: UInt16

    internal let GENERIC: UInt16

    internal let ISCONST: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetKeywordPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAnnotations(): std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>> { /* compiled code */ }

    internal func GetModifiers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>> { /* compiled code */ }

    internal func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }

    internal func GetIsConst(): Bool { /* compiled code */ }
}

public class NodeFormat_VarWithPatternDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let TYPE: UInt16

    internal let COLON_POS: UInt16

    internal let INITIALIZER: UInt16

    internal let PATTERN: UInt16

    internal let ASSIGN_POS: UInt16

    internal let IS_VAR: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsVar(): Bool { /* compiled code */ }
}

public class NodeFormat_FuncDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_PAREN_POS: UInt16

    internal let RIGHT_PAREN_POS: UInt16

    internal let FUNC_BODY: UInt16

    internal let IS_SETTER: UInt16

    internal let IS_GETTER: UInt16

    internal let OP_KIND: UInt16

    internal let ISENUMCONSTRUCT: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }

    internal func GetIsSetter(): Bool { /* compiled code */ }

    internal func GetIsGetter(): Bool { /* compiled code */ }

    internal func GetOpKind(): Int32 { /* compiled code */ }

    internal func GetIsEnumConstruct(): Bool { /* compiled code */ }
}

public class NodeFormat_MainDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let FUNC_BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

public class NodeFormat_MacroDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFTPARENPOS: UInt16

    internal let RIGHTPARENPOS: UInt16

    internal let FUNC_BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

public class NodeFormat_StructBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let DECLS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_Decl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let DECL_TYPE: UInt16

    internal let DECL: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetDeclType(): std.ast.NodeFormat_AnyDecl { /* compiled code */ }

    internal func GetDeclAsFuncDecl(): std.core.Option<std.ast.NodeFormat_FuncDecl> { /* compiled code */ }

    internal func GetDeclAsMainDecl(): std.core.Option<std.ast.NodeFormat_MainDecl> { /* compiled code */ }

    internal func GetDeclAsVarDecl(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    internal func GetDeclAsVarWithPatternDecl(): std.core.Option<std.ast.NodeFormat_VarWithPatternDecl> { /* compiled code */ }

    internal func GetDeclAsStructDecl(): std.core.Option<std.ast.NodeFormat_StructDecl> { /* compiled code */ }

    internal func GetDeclAsClassDecl(): std.core.Option<std.ast.NodeFormat_ClassDecl> { /* compiled code */ }

    internal func GetDeclAsInterfaceDecl(): std.core.Option<std.ast.NodeFormat_InterfaceDecl> { /* compiled code */ }

    internal func GetDeclAsGenericParamDecl(): std.core.Option<std.ast.NodeFormat_GenericParamDecl> { /* compiled code */ }

    internal func GetDeclAsPrimaryCtorDecl(): std.core.Option<std.ast.NodeFormat_PrimaryCtorDecl> { /* compiled code */ }

    internal func GetDeclAsPropDecl(): std.core.Option<std.ast.NodeFormat_PropDecl> { /* compiled code */ }

    internal func GetDeclAsEnumDecl(): std.core.Option<std.ast.NodeFormat_EnumDecl> { /* compiled code */ }

    internal func GetDeclAsTypeAliasDecl(): std.core.Option<std.ast.NodeFormat_TypeAliasDecl> { /* compiled code */ }

    internal func GetDeclAsExtendDecl(): std.core.Option<std.ast.NodeFormat_ExtendDecl> { /* compiled code */ }

    internal func GetDeclAsMacroDecl(): std.core.Option<std.ast.NodeFormat_MacroDecl> { /* compiled code */ }

    internal func GetDeclAsMacroExpandDecl(): std.core.Option<std.ast.NodeFormat_MacroExpandDecl> { /* compiled code */ }

    internal func GetDeclAsFuncParam(): std.core.Option<std.ast.NodeFormat_FuncParam> { /* compiled code */ }
}

public class NodeFormat_StructDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let BODY: UInt16

    internal let UPPER_BOUND_POS: UInt16

    internal let SUPER_TYPES: UInt16

    internal let GENERIC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_StructBody> { /* compiled code */ }

    internal func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

public class NodeFormat_ClassBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let DECLS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_ClassDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let UPPER_BOUND_POS: UInt16

    internal let SUPER_TYPES: UInt16

    internal let BODY: UInt16

    internal let SUB_DECLS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_ClassBody> { /* compiled code */ }

    internal func GetSubDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

public class NodeFormat_InterfaceBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let DECLS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_InterfaceDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let UPPER_BOUND_POS: UInt16

    internal let SUPER_TYPES: UInt16

    internal let BODY: UInt16

    internal let SUB_DECLS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetBody(): std.core.Option<std.ast.NodeFormat_InterfaceBody> { /* compiled code */ }

    internal func GetSubDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

public class NodeFormat_EnumDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let HAS_ARGUMENTS: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let CONSTRUCTORS: UInt16

    internal let BIT_OR_POS_VEC: UInt16

    internal let MEMBERS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal let UPPER_BOUND_POS: UInt16

    internal let SUPER_INTERFACE_TYPES: UInt16

    internal let HASELLIPSIS: UInt16

    internal let ELLIPSIS_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetHasArguments(): Bool { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetConstructors(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetMembers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetSuperInterfaceTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetHasEllipsis(): Bool { /* compiled code */ }

    internal func GetEllipsisPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_GenericParamDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let COMMA_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_PrimaryCtorDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let FUNC_BODY: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

public class NodeFormat_PropDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let COLON_POS: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let GETTERS: UInt16

    internal let SETTERS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    internal func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetGetters(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>> { /* compiled code */ }

    internal func GetSetters(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_GenericConstraint <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let WHERE_POS: UInt16

    internal let TYPE: UInt16

    internal let OPERATOR_POS: UInt16

    internal let UPPER_BOUND: UInt16

    internal let BIT_AND_POS_VEC: UInt16

    internal let COMMA_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetWherePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_RefType> { /* compiled code */ }

    internal func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetUpperBound(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetBitAndPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_Annotation <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let KIND: UInt16

    internal let IDENTPOS: UInt16

    internal let IDENTIFIER: UInt16

    internal let ARGS: UInt16

    internal let OVERFLOW_STRATEGY: UInt16

    internal let ATTRS: UInt16

    internal let CONDEXPR: UInt16

    internal let LEFT_SQUARE_POS: UInt16

    internal let RIGHT_SQUARE_POS: UInt16

    internal let IS_COMPILE_TIME_VISIBLE: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetKind(): UInt16 { /* compiled code */ }

    internal func GetIdentPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    internal func GetOverflowStrategy(): std.core.String { /* compiled code */ }

    internal func GetAttrs(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    internal func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetIsCompileTimeVisible(): Bool { /* compiled code */ }
}

public class NodeFormat_Modifier <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let KIND: UInt16

    internal let IS_EXPLICIT: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetKind(): UInt16 { /* compiled code */ }

    internal func GetIsExplicit(): Bool { /* compiled code */ }
}

public class NodeFormat_TypeAliasDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let ASSIGN_POS: UInt16

    internal let TYPE: UInt16

    internal let GENERIC: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

public class NodeFormat_ExtendDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let EXTENDED_TYPE: UInt16

    internal let UPPER_BOUND_POS: UInt16

    internal let INTERFACES: UInt16

    internal let WHERE_POS: UInt16

    internal let LEFT_CURL_POS: UInt16

    internal let MEMBERS: UInt16

    internal let RIGHT_CURL_POS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetExtendedType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetInterfaces(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    internal func GetWherePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetMembers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

public class NodeFormat_MacroExpandDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let INVOCATION: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    internal func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }
}

public class NodeFormat_Node <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let ROOT_TYPE: UInt16

    internal let ROOT: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetRootType(): std.ast.NodeFormat_AnyNode { /* compiled code */ }

    internal func GetRootAsExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    internal func GetRootAsDecl(): std.core.Option<std.ast.NodeFormat_Decl> { /* compiled code */ }

    internal func GetRootAsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    internal func GetRootAsPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    internal func GetRootAsFile(): std.core.Option<std.ast.NodeFormat_File> { /* compiled code */ }

    internal func GetRootAsPackageSpec(): std.core.Option<std.ast.NodeFormat_PackageSpec> { /* compiled code */ }

    internal func GetRootAsImportSpec(): std.core.Option<std.ast.NodeFormat_ImportSpec> { /* compiled code */ }

    internal func GetRootAsAnnotation(): std.core.Option<std.ast.NodeFormat_Annotation> { /* compiled code */ }
}

public class NodeFormat_MacroInvocation <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let FULLNAME: UInt16

    internal let IDENTIFIER: UInt16

    internal let IDENTIFIERPOS: UInt16

    internal let LEFTSQUAREPOS: UInt16

    internal let RIGHTSQUAREPOS: UInt16

    internal let LEFTPARENPOS: UInt16

    internal let RIGHTPARENPOS: UInt16

    internal let ATPOS: UInt16

    internal let ATTRS: UInt16

    internal let ARGS: UInt16

    internal let DECL: UInt16

    internal let HASPARENTHESIS: UInt16

    internal let IS_COMPILE_TIME_VISIBLE: UInt16

    internal func GetFullName(): std.core.String { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAtPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAttrs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }

    internal func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }

    internal func GetDecl(): std.core.Option<std.ast.NodeFormat_Decl> { /* compiled code */ }

    internal func GetHasParenthesis(): Bool { /* compiled code */ }

    internal func GetIsCompileTimeVisible(): Bool { /* compiled code */ }
}

public class NodeFormat_File <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let FILENAME: UInt16

    internal let FILEPATH: UInt16

    internal let PACKAGE: UInt16

    internal let IMPORTS: UInt16

    internal let DECLS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetFileName(): std.core.String { /* compiled code */ }

    internal func GetFilePath(): std.core.String { /* compiled code */ }

    internal func GetPackage(): std.core.Option<std.ast.NodeFormat_PackageSpec> { /* compiled code */ }

    internal func GetImports(): std.core.Array<std.core.Option<std.ast.NodeFormat_ImportSpec>> { /* compiled code */ }

    internal func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

public class NodeFormat_PackageSpec <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let MACROPOS: UInt16

    internal let PACKAGEPOS: UInt16

    internal let PREFIXPATHS: UInt16

    internal let PREFIXPOSES: UInt16

    internal let PREFIXDOTPOSES: UInt16

    internal let PACKAGENAME: UInt16

    internal let PACKAGENAMEPOS: UInt16

    internal let ACCESSIBLE: UInt16

    internal let HASMACRO: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetMacroPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPackagePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetPrefixPaths(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    internal func GetPrefixPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetPrefixDotPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetPackageName(): std.core.String { /* compiled code */ }

    internal func GetPackageNamePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAccessible(): std.ast.NodeFormat_AccessibleKind { /* compiled code */ }

    internal func GetHasMacro(): Bool { /* compiled code */ }
}

public class NodeFormat_ImportSpec <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let REEXPORT: UInt16

    internal let IMPORTPOS: UInt16

    internal let CONTENT: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetReExport(): std.ast.NodeFormat_ReExportKind { /* compiled code */ }

    internal func GetImportPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetContent(): std.core.Option<std.ast.NodeFormat_ImportContent> { /* compiled code */ }
}

public class NodeFormat_ImportContent <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    internal let BASE: UInt16

    internal let KIND: UInt16

    internal let PREFIXPATHS: UInt16

    internal let PREFIXPOSES: UInt16

    internal let PREFIXDOTPOSES: UInt16

    internal let IDENTIFIER: UInt16

    internal let IDENTIFIERPOS: UInt16

    internal let ASPOS: UInt16

    internal let ASIDENTIFIER: UInt16

    internal let ASIDENTIFIERPOS: UInt16

    internal let LEFTCURLPOS: UInt16

    internal let ITEMS: UInt16

    internal let COMMAPOSES: UInt16

    internal let RIGHTCURLPOS: UInt16

    internal func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    internal func GetKind(): std.ast.NodeFormat_ImportKind { /* compiled code */ }

    internal func GetPrefixPaths(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    internal func GetPrefixPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetPrefixDotPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetIdentifier(): std.core.String { /* compiled code */ }

    internal func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAsPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetAsIdentifier(): std.core.String { /* compiled code */ }

    internal func GetAsIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    internal func GetItems(): std.core.Array<std.core.Option<std.ast.NodeFormat_ImportContent>> { /* compiled code */ }

    internal func GetCommaPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    internal func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

internal func isValidPosition(pos: std.ast.NodeFormat_Position): Bool { /* compiled code */ }

internal func isValidPosition(tk: std.ast.Token): Bool { /* compiled code */ }

public struct Token <: std.ast.ToBytes {
    public let kind: std.ast.TokenKind

    public let value: std.core.String

    public let pos: std.ast.Position

    public var delimiterNum: UInt16

    protected var isSingleQuote: Bool

    init() { /* compiled code */ }

    init(kind: std.ast.TokenKind) { /* compiled code */ }

    init(kind: std.ast.TokenKind, value: std.core.String) { /* compiled code */ }

    init(k: std.ast.TokenKind, v: std.core.String, f: UInt32, l: Int32, c: Int32) { /* compiled code */ }

    public func addPosition(fileID: UInt32, line: Int32, colum: Int32): std.ast.Token { /* compiled code */ }

    internal func addPosition(position: std.ast.NodeFormat_Position): std.ast.Token { /* compiled code */ }

    internal func addPosition(position: std.ast.Position): std.ast.Token { /* compiled code */ }

    public operator func ==(r: std.ast.Token): Bool { /* compiled code */ }

    public operator func !=(r: std.ast.Token): Bool { /* compiled code */ }

    public operator func +(r: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    public operator func +(r: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    public func dump(): Unit { /* compiled code */ }

    public func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    internal func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64 { /* compiled code */ }

    internal func getSize(): Int64 { /* compiled code */ }
}

internal func isValidToken(tk: std.ast.Token): Bool { /* compiled code */ }

internal func getNodeBeginPos(nodeBase: std.core.Option<std.ast.NodeFormat_NodeBase>): std.ast.NodeFormat_Position { /* compiled code */ }

internal func getOperaterKindOrIdentTokens(opKind: Int32, pos: std.ast.NodeFormat_Position, ident: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

public open class Tokens <: std.core.Object & std.core.ToString & std.core.Iterable<std.ast.Token> & std.ast.ToBytes {
    protected var tokens: std.collection.ArrayList<std.ast.Token>

    internal func refreshPosition(): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }

    init(buf: std.core.Array<UInt8>) { /* compiled code */ }

    init(tokArray: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(tokArrayList: std.collection.ArrayList<std.ast.Token>) { /* compiled code */ }

    public open prop size: Int64 {
        get() { /* compiled code */ }
    }

    public open func get(index: Int64): std.ast.Token { /* compiled code */ }

    public func iterator(): std.ast.TokensIterator { /* compiled code */ }

    public func concat(tokens: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    public operator func [](index: Int64): std.ast.Token { /* compiled code */ }

    public open operator func [](range: std.core.Range<Int64>): std.ast.Tokens { /* compiled code */ }

    public operator func +(r: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    public operator func +(r: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    public open func append(tokens: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    public open func append(token: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    public func append(node: std.ast.Node): std.ast.Tokens { /* compiled code */ }

    public func remove(index: Int64): std.ast.Tokens { /* compiled code */ }

    public func dump(): Unit { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    internal func toBytes(startFrom: Int64): std.core.Array<UInt8> { /* compiled code */ }

    internal func getSize(startFrom!: Int64 = COMPILED_CODE /* compiled code */): Int64 { /* compiled code */ }
}

internal func getIndent(indent: UInt16): std.core.String { /* compiled code */ }

internal func getTokenIndent(name: std.core.String, tk: std.ast.Token, indent: UInt16, noNameIndent!: Bool = COMPILED_CODE /* compiled code */, index!: std.core.Option<Int64> = COMPILED_CODE /* compiled code */): std.core.String { /* compiled code */ }

internal func dumpNodes<N>(name: std.core.String, contents: std.collection.ArrayList<N>, indent: UInt16): std.core.String { /* compiled code */ }

public abstract open class Node <: std.core.Object & std.ast.ToTokens {
    init() { /* compiled code */ }

    protected var begin_: std.ast.Position

    protected var end_: std.ast.Position

    protected var astKind: std.core.String

    protected var currentIndent: UInt16

    protected var beginNode: Bool

    public mut prop beginPos: std.ast.Position {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop endPos: std.ast.Position {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens

    public func traverse(v: std.ast.Visitor): Unit

    public func dump(): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String
}

public func compareTokens(tokens1: std.ast.Tokens, tokens2: std.ast.Tokens): Bool { /* compiled code */ }

internal func checkKindValue(k: std.ast.TokenKind, v: std.core.String): Bool { /* compiled code */ }

public enum TokenKind <: std.core.ToString {
    | DOT

    | COMMA

    | LPAREN

    | RPAREN

    | LSQUARE

    | RSQUARE

    | LCURL

    | RCURL

    | EXP

    | MUL

    | MOD

    | DIV

    | ADD

    | SUB

    | INCR

    | DECR

    | AND

    | OR

    | COALESCING

    | PIPELINE

    | COMPOSITION

    | NOT

    | BITAND

    | BITOR

    | BITXOR

    | BITNOT

    | LSHIFT

    | RSHIFT

    | COLON

    | SEMI

    | ASSIGN

    | ADD_ASSIGN

    | SUB_ASSIGN

    | MUL_ASSIGN

    | EXP_ASSIGN

    | DIV_ASSIGN

    | MOD_ASSIGN

    | AND_ASSIGN

    | OR_ASSIGN

    | BITAND_ASSIGN

    | BITOR_ASSIGN

    | BITXOR_ASSIGN

    | LSHIFT_ASSIGN

    | RSHIFT_ASSIGN

    | ARROW

    | BACKARROW

    | DOUBLE_ARROW

    | RANGEOP

    | CLOSEDRANGEOP

    | ELLIPSIS

    | HASH

    | AT

    | QUEST

    | LT

    | GT

    | LE

    | GE

    | IS

    | AS

    | NOTEQ

    | EQUAL

    | WILDCARD

    | INT8

    | INT16

    | INT32

    | INT64

    | INTNATIVE

    | UINT8

    | UINT16

    | UINT32

    | UINT64

    | UINTNATIVE

    | FLOAT16

    | FLOAT32

    | FLOAT64

    | RUNE

    | BOOLEAN

    | NOTHING

    | UNIT

    | STRUCT

    | ENUM

    | VARRAY

    | THISTYPE

    | PACKAGE

    | IMPORT

    | CLASS

    | INTERFACE

    | FUNC

    | MACRO

    | QUOTE

    | DOLLAR

    | LET

    | VAR

    | CONST

    | TYPE

    | INIT

    | THIS

    | SUPER

    | IF

    | ELSE

    | CASE

    | TRY

    | CATCH

    | FINALLY

    | FOR

    | DO

    | WHILE

    | THROW

    | RETURN

    | CONTINUE

    | BREAK

    | IN

    | NOT_IN

    | MATCH

    | WHERE

    | EXTEND

    | WITH

    | PROP

    | STATIC

    | PUBLIC

    | PRIVATE

    | INTERNAL

    | PROTECTED

    | OVERRIDE

    | REDEF

    | ABSTRACT

    | SEALED

    | OPEN

    | FOREIGN

    | INOUT

    | MUT

    | UNSAFE

    | OPERATOR

    | SPAWN

    | SYNCHRONIZED

    | UPPERBOUND

    | MAIN

    | IDENTIFIER

    | PACKAGE_IDENTIFIER

    | INTEGER_LITERAL

    | RUNE_BYTE_LITERAL

    | FLOAT_LITERAL

    | COMMENT

    | NL

    | END

    | SENTINEL

    | RUNE_LITERAL

    | STRING_LITERAL

    | SINGLE_QUOTED_STRING_LITERAL

    | JSTRING_LITERAL

    | MULTILINE_STRING

    | MULTILINE_RAW_STRING

    | BOOL_LITERAL

    | UNIT_LITERAL

    | DOLLAR_IDENTIFIER

    | ANNOTATION

    | AT_EXCL

    | ILLEGAL

    public operator func ==(right: std.ast.TokenKind): Bool { /* compiled code */ }

    public operator func !=(right: std.ast.TokenKind): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func opPrecedence(): Int64 { /* compiled code */ }

    internal func isNonAssociative(): Bool { /* compiled code */ }
}

internal func getImportSpecTokens(imports: std.collection.ArrayList<std.ast.ImportList>): std.ast.Tokens { /* compiled code */ }

public class ImportList <: std.ast.Node {
    init(reExportToken: std.ast.Token, imp: std.ast.Token, content: std.ast.ImportContent) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var reExportToken_: std.ast.Token

    private var import_: std.ast.Token

    private var content_: std.ast.ImportContent

    private let innerToken_: std.ast.Tokens

    public mut prop modifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop content: std.ast.ImportContent {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isImportMulti(): Bool { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func dumpCommonContent(currentIndent: UInt16, upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, body: std.ast.Body): std.core.String { /* compiled code */ }

public open class TypeNode <: std.ast.Node {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var typeParameterName_: std.ast.Token

    private var colon_: std.ast.Token

    public mut prop typeParameterName: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public open func toTokens(): std.ast.Tokens { /* compiled code */ }

    public open func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected open func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class GenericConstraint <: std.ast.Node {
    init(keyword: std.ast.Token, ty: std.ast.TypeNode, upbound: std.ast.Token, upperBounds: std.collection.ArrayList<std.ast.TypeNode>, bitAnds: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    private var ty_: std.ast.TypeNode

    private var upbound_: std.ast.Token

    private var upperBounds_: std.collection.ArrayList<std.ast.TypeNode>

    private var bitAnds_: std.ast.Tokens

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArgument: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop upperBounds: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop bitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Body <: std.ast.Node {
    init(lBrace: std.ast.Token, decls: std.collection.ArrayList<std.ast.Decl>, rBrace: std.ast.Token) { /* compiled code */ }

    init(decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lBrace_: std.ast.Token

    private var decls_: std.collection.ArrayList<std.ast.Decl>

    private var rBrace_: std.ast.Token

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func referenceType2Tokens(upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, constraintCommas: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

extend std.ast.Tokens {
    internal func tryGet(index: Int64): std.core.Option<std.ast.Token> { /* compiled code */ }
}

extend std.ast.Token {
    internal func tryGet(): std.core.Option<std.ast.Token> { /* compiled code */ }
}

internal func checkTokenType(tk: std.ast.Token, kind: std.ast.TokenKind): Unit { /* compiled code */ }

internal func checkTokensType(tks: std.ast.Tokens, kind: std.ast.TokenKind): Unit { /* compiled code */ }

internal func checkTokenTypeAssign(tk: std.ast.Token): Unit { /* compiled code */ }

internal func checkPrimitiveType(tk: std.ast.Token): Unit { /* compiled code */ }

internal func checkValid(input: std.ast.Token): std.ast.Token { /* compiled code */ }

public open class Decl <: std.ast.Node {
    init(annotations: std.collection.ArrayList<std.ast.Annotation>, modifiers: std.collection.ArrayList<std.ast.Modifier>, keyword: std.ast.Token, identifier: std.ast.Token, genericParam: std.core.Option<std.ast.GenericParam>, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, constraintCommas: std.ast.Tokens, isGenericDecl: Bool) { /* compiled code */ }

    init(keyword: std.ast.Token, identifier: std.ast.Token) { /* compiled code */ }

    init(decl: std.ast.Decl) { /* compiled code */ }

    init(input: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var annotations_: std.collection.ArrayList<std.ast.Annotation>

    protected var modifiers_: std.collection.ArrayList<std.ast.Modifier>

    protected var keyword_: std.ast.Token

    protected var identifier_: std.ast.Token

    private var genericParam_: std.core.Option<std.ast.GenericParam>

    private var constraint_: std.collection.ArrayList<std.ast.GenericConstraint>

    private var constraintCommas_: std.ast.Tokens

    private var isGenericDecl_: Bool

    protected var node: std.ast.Node

    public open mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop annotations: std.collection.ArrayList<std.ast.Annotation> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop modifiers: std.collection.ArrayList<std.ast.Modifier> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop genericParam: std.ast.GenericParam {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop genericConstraint: std.collection.ArrayList<std.ast.GenericConstraint> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop constraintCommas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func hasAttr(attr: std.core.String): Bool { /* compiled code */ }

    public func getAttrs(): std.ast.Tokens { /* compiled code */ }

    public mut prop isGenericDecl: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public open func toTokens(): std.ast.Tokens { /* compiled code */ }

    public open func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected open func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Annotation <: std.ast.Node {
    init(at: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rSquare: std.ast.Token, attrs: std.ast.Tokens, cond: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var innerToken_: std.ast.Tokens

    private var at_: std.ast.Token

    private var identifier_: std.ast.Token

    private var lSquare_: std.ast.Token

    private var args_: std.collection.ArrayList<std.ast.Argument>

    private var rSquare_: std.ast.Token

    private var attrs_: std.ast.Tokens

    private var cond_: std.core.Option<std.ast.Expr>

    public mut prop at: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop attributes: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Modifier <: std.ast.Node {
    init(keyword: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var kind_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class GenericParam <: std.ast.Node {
    init(lAngle: std.ast.Token, parameters: std.ast.Tokens, rAngle: std.ast.Token, commas: std.ast.Tokens) { /* compiled code */ }

    init(parameters: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lAngle_: std.ast.Token

    private var parameters_: std.ast.Tokens

    private var rAngle_: std.ast.Token

    private var commas_: std.ast.Tokens

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop parameters: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public abstract class Visitor {
    private var breakTraverseFlag: Bool

    protected func needBreakTraverse(): Bool { /* compiled code */ }

    public func breakTraverse(): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Node): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Expr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.IsExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.AsExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.LitConstExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.UnaryExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.BinaryExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ParenExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.CallExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.RefExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ReturnExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.AssignExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MemberAccess): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.IfExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.LetPatternExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MatchExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.WhileExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.LambdaExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.SpawnExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.SynchronizedExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TrailingClosureExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TypeConvExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ForInExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PrimitiveTypeExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ArrayLiteral): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TupleLiteral): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.SubscriptExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.RangeExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Block): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.DoWhileExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.JumpExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.IncOrDecExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TryExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ThrowExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.OptionalExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.QuoteExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MacroExpandExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.WildcardExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.VArrayExpr): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Decl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ClassDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.FuncDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MainDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MacroDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.StructDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.InterfaceDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PropDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.VarDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PrimaryCtorDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.EnumDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.FuncParam): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TypeAliasDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ExtendDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MacroExpandDecl): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TypeNode): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.RefType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PrimitiveType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.FuncType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ThisType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ParenType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.QualifiedType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PrefixType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TupleType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.VArrayType): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Pattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ConstPattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.WildcardPattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.VarPattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.VarOrEnumPattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ExceptTypePattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TypePattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.EnumPattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.TuplePattern): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.GenericParam): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.GenericConstraint): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.MatchCase): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Program): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.PackageHeader): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ImportList): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.ImportContent): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Constructor): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Argument): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Annotation): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Modifier): Unit { /* compiled code */ }

    protected open func visit(_: std.ast.Body): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

public class ClassDecl <: std.ast.Decl {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init() { /* compiled code */ }

    private var upBound_: std.ast.Token

    private var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    private var superTypeBitAnds_: std.ast.Tokens

    private var classbody_: std.ast.Body

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class StructDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var upBound_: std.ast.Token

    private var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    private var superTypeBitAnds_: std.ast.Tokens

    private var structbody_: std.ast.Body

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class InterfaceDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var upBound_: std.ast.Token

    private var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    private var superTypeBitAnds_: std.ast.Tokens

    private var interfacetbody_: std.ast.Body

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class EnumDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, lBrace: std.ast.Token, bitor: std.ast.Tokens, constructors: std.collection.ArrayList<std.ast.Constructor>, decls: std.collection.ArrayList<std.ast.Decl>, rBrace: std.ast.Token, ellipsis: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var upBound_: std.ast.Token

    private var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    private var superTypeBitAnds_: std.ast.Tokens

    private var lBrace_: std.ast.Token

    private var bitor_: std.ast.Tokens

    private var constructors_: std.collection.ArrayList<std.ast.Constructor>

    private var decls_: std.collection.ArrayList<std.ast.Decl>

    private var rBrace_: std.ast.Token

    private var ellipsis_: std.ast.Token

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop constructors: std.collection.ArrayList<std.ast.Constructor> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop ellipsis: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Constructor <: std.ast.Node {
    init() { /* compiled code */ }

    init(annotations: std.collection.ArrayList<std.ast.Annotation>, identifier: std.ast.Token) { /* compiled code */ }

    init(annotations: std.collection.ArrayList<std.ast.Annotation>, identifier: std.ast.Token, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    private var identifier_: std.ast.Token

    private var lParen_: std.ast.Token

    private var params_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    private var annotations_: std.collection.ArrayList<std.ast.Annotation>

    public mut prop annotations: std.collection.ArrayList<std.ast.Annotation> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ExtendDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, extendType: std.ast.TypeNode, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var extendType_: std.ast.TypeNode

    private var upBound_: std.ast.Token

    private var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    private var superTypeBitAnds_: std.ast.Tokens

    private var body_: std.ast.Body

    public override mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop extendType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class FuncDecl <: std.ast.Decl {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(decl: std.ast.Decl, lParen: std.ast.Token, funcParams: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init() { /* compiled code */ }

    private var overloadedOperators_: std.ast.Tokens

    private var lParen_: std.ast.Token

    private var params_: std.collection.ArrayList<std.ast.FuncParam>

    private var rParen_: std.ast.Token

    private var colon_: std.ast.Token

    private var retTy_: std.core.Option<std.ast.TypeNode>

    private var block_: std.ast.Block

    internal var isEnumConstruct: Bool

    public mut prop overloadOp: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isConst(): Bool { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public open class FuncParam <: std.ast.Decl {
    init(decl: std.ast.Decl, notMark: std.ast.Token, colon: std.ast.Token, paramType: std.ast.TypeNode, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>, comma: std.ast.Token, isMemberParam: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(key: std.ast.Token, identifier: std.ast.Token) { /* compiled code */ }

    private var not_: std.ast.Token

    private var colon_: std.ast.Token

    private var paramType_: std.ast.TypeNode

    private var assign_: std.ast.Token

    private var expr_: std.core.Option<std.ast.Expr>

    private var comma_: std.ast.Token

    private var isMemberParam_: Bool

    public mut prop not: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop paramType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isMemberParam(): Bool { /* compiled code */ }

    public open func toTokens(): std.ast.Tokens { /* compiled code */ }

    public open func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected open func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Block <: std.ast.Expr {
    init(lCurl: std.ast.Token, nodes: std.collection.ArrayList<std.ast.Node>, rCurl: std.ast.Token, unsafe_: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lCurl_: std.ast.Token

    private var nodes_: std.collection.ArrayList<std.ast.Node>

    private var rCurl_: std.ast.Token

    private var unsafe_: std.ast.Token

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop nodes: std.collection.ArrayList<std.ast.Node> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MainDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, funcParams: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var rParen_: std.ast.Token

    private var colon_: std.ast.Token

    private var params_: std.collection.ArrayList<std.ast.FuncParam>

    private var retTy_: std.core.Option<std.ast.TypeNode>

    private var block_: std.ast.Block

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MacroDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var rParen_: std.ast.Token

    private var params_: std.collection.ArrayList<std.ast.FuncParam>

    private var colon_: std.ast.Token

    private var retTy_: std.core.Option<std.ast.TypeNode>

    private var block_: std.ast.Block

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class PrimaryCtorDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var rParen_: std.ast.Token

    private var params_: std.collection.ArrayList<std.ast.FuncParam>

    private var block_: std.ast.Block

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isConst(): Bool { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class VarDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, colon: std.ast.Token, declType: std.core.Option<std.ast.TypeNode>, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(decl: std.ast.Decl, pattern: std.core.Option<std.ast.Pattern>, colon: std.ast.Token, declType: std.core.Option<std.ast.TypeNode>, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var pattern_: std.core.Option<std.ast.Pattern>

    private var colon_: std.ast.Token

    private var declType_: std.core.Option<std.ast.TypeNode>

    private var assign_: std.ast.Token

    private var expr_: std.core.Option<std.ast.Expr>

    internal var isEnumConstruct: Bool

    public mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isConst(): Bool { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public open class Expr <: std.ast.Node {
    init() { /* compiled code */ }

    public open func toTokens(): std.ast.Tokens { /* compiled code */ }

    public open func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected open func dump(_: UInt16): std.core.String { /* compiled code */ }

    protected open func precedence(): Int64 { /* compiled code */ }
}

public open class Pattern <: std.ast.Node {
    init() { /* compiled code */ }

    public open func toTokens(): std.ast.Tokens { /* compiled code */ }

    public open func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected open func dump(_: UInt16): std.core.String { /* compiled code */ }
}

public class PropDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, colon: std.ast.Token, declType: std.ast.TypeNode, lCurl: std.ast.Token, getter: std.core.Option<std.ast.FuncDecl>, setter: std.core.Option<std.ast.FuncDecl>, rCurl: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var colon_: std.ast.Token

    private var declType_: std.ast.TypeNode

    private var lCurl_: std.ast.Token

    private var getter_: std.core.Option<std.ast.FuncDecl>

    private var setter_: std.core.Option<std.ast.FuncDecl>

    private var rCurl_: std.ast.Token

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop getter: std.ast.FuncDecl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop setter: std.ast.FuncDecl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TypeAliasDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, assign: std.ast.Token, aliasType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var assign_: std.ast.Token

    private var aliasType_: std.ast.TypeNode

    public mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop aliasType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MacroExpandDecl <: std.ast.Decl {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool, isCompileTimeVisible: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var pkgIdentifier_: std.ast.Token

    private var lSquare_: std.ast.Token

    private var macroAttrs_: std.ast.Tokens

    private var rSquare_: std.ast.Token

    private var lParen_: std.ast.Token

    private var macroInputs_: std.ast.Tokens

    private var rParen_: std.ast.Token

    private var macroInputDecl_: std.core.Option<std.ast.Decl>

    private var hasParenthesis_: Bool

    internal var isCompileTimeVisible_: Bool

    public mut prop fullIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroInputDecl: std.ast.Decl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MacroExpandParam <: std.ast.FuncParam {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool, isCompileTimeVisible: Bool) { /* compiled code */ }

    init() { /* compiled code */ }

    private var pkgIdentifier_: std.ast.Token

    private var lSquare_: std.ast.Token

    private var macroAttrs_: std.ast.Tokens

    private var rSquare_: std.ast.Token

    private var lParen_: std.ast.Token

    private var macroInputs_: std.ast.Tokens

    private var rParen_: std.ast.Token

    private var macroInputDecl_: std.core.Option<std.ast.Decl>

    private var hasParenthesis_: Bool

    internal var isCompileTimeVisible_: Bool

    public mut prop fullIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroInputDecl: std.ast.Decl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func boundsCheck(buf: std.core.Array<UInt8>, sz: Int64): Bool { /* compiled code */ }

internal func getUInt8(buf: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

internal func getBool(buf: std.core.Array<UInt8>): Bool { /* compiled code */ }

internal func getUInt16(buf: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

internal func getUInt32(buf: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

internal func getInt32(buf: std.core.Array<UInt8>): Int32 { /* compiled code */ }

internal func getUInt64(buf: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

internal func getString(buf: std.core.Array<UInt8>, offset: Int64, len: UInt32): std.core.String { /* compiled code */ }

public enum DiagReportLevel {
    | ERROR

    | WARNING

    public func level(): Int32 { /* compiled code */ }
}

public func diagReport(level: std.ast.DiagReportLevel, tokens: std.ast.Tokens, message: std.core.String, hint: std.core.String): Unit { /* compiled code */ }

public class ASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class ParseASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class BinaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(leftExpr: std.ast.Expr, op: std.ast.Token, rightExpr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lExpr_: std.ast.Expr

    private var rExpr_: std.ast.Expr

    private var op_: std.ast.Token

    public mut prop leftExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rightExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class UnaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(op: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    private var expr_: std.ast.Expr

    private var op_: std.ast.Token

    public mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class IsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, shiftType: std.ast.TypeNode) { /* compiled code */ }

    private var expr_: std.ast.Expr

    private var op_: std.ast.Token

    private var shiftType_: std.ast.TypeNode

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop shiftType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class AsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    private var expr_: std.ast.Expr

    private var op_: std.ast.Token

    private var shiftType_: std.ast.TypeNode

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop shiftType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ParenExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var expr_: std.ast.Expr

    private var rParen_: std.ast.Token

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop parenthesizedExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class LitConstExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(literal: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var literal_: std.ast.Token

    public mut prop literal: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class RefExpr <: std.ast.Expr {
    init(identifier: std.ast.Token, lAngle: std.ast.Token, typeArgs: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var identifier_: std.ast.Token

    private var lAngle_: std.ast.Token

    private var typeArgs_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rAngle_: std.ast.Token

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ReturnExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var expr_: std.core.Option<std.ast.Expr>

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ThrowExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var expr_: std.ast.Expr

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class AssignExpr <: std.ast.Expr {
    init(lexpr: std.ast.Expr, assign: std.ast.Token, rexpr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var assign_: std.ast.Token

    private var lexpr_: std.ast.Expr

    private var rexpr_: std.ast.Expr

    public mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop leftExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rightExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class CallExpr <: std.ast.Expr {
    init(callSuffix: std.ast.Expr, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var postfixExpr_: std.ast.Expr

    private var lParen_: std.ast.Token

    private var args_: std.collection.ArrayList<std.ast.Argument>

    private var rParen_: std.ast.Token

    public mut prop callFunc: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class Argument <: std.ast.Node {
    init(inout_: std.ast.Token, identifier: std.ast.Token, colon: std.ast.Token, expr: std.ast.Expr, comma: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var inout_: std.ast.Token

    private var identifier_: std.ast.Token

    private var colon_: std.ast.Token

    private var expr_: std.ast.Expr

    private var comma_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MemberAccess <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(postfixExpr: std.ast.Expr, dot: std.ast.Token, ident: std.ast.Token, lAngle: std.ast.Token, typeArgs: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    private var postfixExpr_: std.ast.Expr

    private var dot_: std.ast.Token

    private var ident_: std.ast.Token

    private var lAngle_: std.ast.Token

    private var typeArgs_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rAngle_: std.ast.Token

    public mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop dot: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop field: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class IfExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWords: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token, ifBlock: std.ast.Block, elseTk: std.ast.Token, elseExpr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    private var if_: std.ast.Token

    private var lParen_: std.ast.Token

    private var cond_: std.ast.Expr

    private var rParen_: std.ast.Token

    private var else_: std.ast.Token

    private var ifBlock_: std.ast.Block

    private var elseExpr_: std.core.Option<std.ast.Expr>

    public mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop ifBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordE: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop elseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class LetPatternExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(key: std.ast.Token, pattern: std.ast.Pattern, backArrow: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    private var key_: std.ast.Token

    private var pattern_: std.ast.Pattern

    private var backArrow_: std.ast.Token

    private var expr_: std.ast.Expr

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop backArrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MatchExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, selector: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, lCurl: std.ast.Token, matchCases: std.collection.ArrayList<std.ast.MatchCase>, rCurl: std.ast.Token) { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var lParen_: std.ast.Token

    private var selector_: std.core.Option<std.ast.Expr>

    private var rParen_: std.ast.Token

    private var lCurl_: std.ast.Token

    private var matchCases_: std.collection.ArrayList<std.ast.MatchCase>

    private var rCurl_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop selector: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop matchCases: std.collection.ArrayList<std.ast.MatchCase> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MatchCase <: std.ast.Node {
    init(case_: std.ast.Token, expr: std.core.Option<std.ast.Expr>, arrow: std.ast.Token, exprOrDecl: std.ast.Block) { /* compiled code */ }

    init(case_: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, bitOrs: std.ast.Tokens, where_: std.ast.Token, patternGuard: std.core.Option<std.ast.Expr>, arrow: std.ast.Token, exprOrDecl: std.ast.Block) { /* compiled code */ }

    init() { /* compiled code */ }

    private var case_: std.ast.Token

    private var expr_: std.core.Option<std.ast.Expr>

    private var patterns_: std.collection.ArrayList<std.ast.Pattern>

    private var bitOrs_: std.ast.Tokens

    private var where_: std.ast.Token

    private var patternGuard_: std.core.Option<std.ast.Expr>

    private var arrow_: std.ast.Token

    private var exprOrDecl_: std.ast.Block

    public mut prop keywordC: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop bitOrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patternGuard: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop arrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class WhileExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var lParen_: std.ast.Token

    private var cond_: std.ast.Expr

    private var rParen_: std.ast.Token

    private var block_: std.ast.Block

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class DoWhileExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(do_: std.ast.Token, block: std.ast.Block, while_: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    private var do_: std.ast.Token

    private var block_: std.ast.Block

    private var while_: std.ast.Token

    private var lParen_: std.ast.Token

    private var cond_: std.ast.Expr

    private var rParen_: std.ast.Token

    public mut prop keywordD: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class LambdaExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(lCurl: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, db_arrow: std.ast.Token, nodes: std.collection.ArrayList<std.ast.Node>, rCurl: std.ast.Token, mockSupported: Bool) { /* compiled code */ }

    private var lCurl_: std.ast.Token

    private var funcParams_: std.collection.ArrayList<std.ast.FuncParam>

    private var db_arrow_: std.ast.Token

    private var nodes_: std.collection.ArrayList<std.ast.Node>

    private var rCurl_: std.ast.Token

    private var mockSupported_: Bool

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop doubleArrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop nodes: std.collection.ArrayList<std.ast.Node> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class SpawnExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, context: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, task: std.ast.LambdaExpr) { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var lParen_: std.ast.Token

    private var context_: std.core.Option<std.ast.Expr>

    private var rParen_: std.ast.Token

    private var task_: std.ast.LambdaExpr

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop threadContext: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lambdaExpr: std.ast.LambdaExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class SynchronizedExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, mutex: std.ast.Expr, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    private var keyWord_: std.ast.Token

    private var lParen_: std.ast.Token

    private var mutex_: std.ast.Expr

    private var rParen_: std.ast.Token

    private var block_: std.ast.Block

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop structuredMutex: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TrailingClosureExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, lambda: std.ast.LambdaExpr) { /* compiled code */ }

    private var expr_: std.ast.Expr

    private var lambda_: std.ast.LambdaExpr

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lambdaExpr: std.ast.LambdaExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TypeConvExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(ty: std.ast.PrimitiveType, lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    private var type_: std.ast.PrimitiveType

    private var lParen_: std.ast.Token

    private var expr_: std.ast.Expr

    private var rParen_: std.ast.Token

    public mut prop targetType: std.ast.PrimitiveType {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class PrimitiveType <: std.ast.TypeNode {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    private var keyword_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ForInExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(for_: std.ast.Token, lParen: std.ast.Token, pattern: std.ast.Pattern, in_: std.ast.Token, expr: std.ast.Expr, where_: std.ast.Token, patternGuard: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    private var for_: std.ast.Token

    private var lParen_: std.ast.Token

    private var pattern_: std.ast.Pattern

    private var in_: std.ast.Token

    private var expr_: std.ast.Expr

    private var where_: std.ast.Token

    private var patternGuard_: std.core.Option<std.ast.Expr>

    private var rParen_: std.ast.Token

    private var block_: std.ast.Block

    public mut prop keywordF: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patternGuard: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class PrimitiveTypeExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(kind: std.ast.Token) { /* compiled code */ }

    private var keyword_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ArrayLiteral <: std.ast.Expr {
    init(lSquare: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lSquare_: std.ast.Token

    private var elements_: std.collection.ArrayList<std.ast.Expr>

    private var commas_: std.ast.Tokens

    private var rSquare_: std.ast.Token

    public mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop elements: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TupleLiteral <: std.ast.Expr {
    init(lParen: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var elements_: std.collection.ArrayList<std.ast.Expr>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop elements: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class RangeExpr <: std.ast.Expr {
    init(start: std.core.Option<std.ast.Expr>, rangeOp: std.ast.Token, end: std.core.Option<std.ast.Expr>, colon: std.ast.Token, step: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var start_: std.core.Option<std.ast.Expr>

    private var rangeOp_: std.ast.Token

    private var endExpr_: std.core.Option<std.ast.Expr>

    private var colon_: std.ast.Token

    private var step_: std.core.Option<std.ast.Expr>

    public mut prop start: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop end: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop step: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class SubscriptExpr <: std.ast.Expr {
    init(postfixExpr: std.ast.Expr, lSquare: std.ast.Token, index: std.collection.ArrayList<std.ast.Expr>, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var postfixExpr_: std.ast.Expr

    private var lSquare_: std.ast.Token

    private var index_: std.collection.ArrayList<std.ast.Expr>

    private var rSquare_: std.ast.Token

    public mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop indexList: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class JumpExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var kind_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class IncOrDecExpr <: std.ast.Expr {
    init(kind: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var op_: std.ast.Token

    private var expr_: std.ast.Expr

    public mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TryExpr <: std.ast.Expr {
    init(try_: std.ast.Token, resourceSpecLParen: std.ast.Token, resourceSpec: std.collection.ArrayList<std.ast.VarDecl>, resourceSpecCommas: std.ast.Tokens, resourceSpecRParen: std.ast.Token, tryBlock: std.ast.Block, catches: std.ast.Tokens, catchLParens: std.ast.Tokens, catchRParens: std.ast.Tokens, patterns: std.collection.ArrayList<std.ast.Pattern>, catchBlocks: std.collection.ArrayList<std.ast.Block>, finally_: std.ast.Token, finallyBlock: std.core.Option<std.ast.Block>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var try_: std.ast.Token

    private var resourceSpecLParen_: std.ast.Token

    private var resourceSpec_: std.collection.ArrayList<std.ast.VarDecl>

    private var resourceSpecCommas_: std.ast.Tokens

    private var resourceSpecRParen_: std.ast.Token

    private var tryBlock_: std.ast.Block

    private var catches_: std.ast.Tokens

    private var catchLParens_: std.ast.Tokens

    private var catchRParens_: std.ast.Tokens

    private var patterns_: std.collection.ArrayList<std.ast.Pattern>

    private var catchBlocks_: std.collection.ArrayList<std.ast.Block>

    private var finally_: std.ast.Token

    private var finallyBlock_: std.core.Option<std.ast.Block>

    public mut prop keywordT: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop resourceSpec: std.collection.ArrayList<std.ast.VarDecl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop tryBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordsC: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop catchPatterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop catchBlocks: std.collection.ArrayList<std.ast.Block> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordF: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop finallyBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class OptionalExpr <: std.ast.Expr {
    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(baseExpr: std.ast.Expr, quest: std.ast.Token) { /* compiled code */ }

    private var baseExpr_: std.ast.Expr

    private var quest_: std.ast.Token

    public mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop quest: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    protected func precedence(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class QuoteToken <: std.ast.Expr {
    init(tokens: std.ast.Tokens) { /* compiled code */ }

    private var tokens_: std.ast.Tokens

    public mut prop tokens: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class QuoteExpr <: std.ast.Expr {
    init(keyword: std.ast.Token, lParen: std.ast.Token, exprs: std.collection.ArrayList<std.ast.Expr>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    private var lParen_: std.ast.Token

    private var exprs_: std.collection.ArrayList<std.ast.Expr>

    private var rParen_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop exprs: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class WildcardExpr <: std.ast.Expr {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class VArrayExpr <: std.ast.Expr {
    init(ty: std.ast.VArrayType, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var ty_: std.ast.VArrayType

    private var lParen_: std.ast.Token

    private var args_: std.collection.ArrayList<std.ast.Argument>

    private var rParen_: std.ast.Token

    public mut prop vArrayType: std.ast.VArrayType {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class VArrayType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token, lAngle: std.ast.Token, elementTy: std.ast.TypeNode, dollar: std.ast.Token, size: std.ast.Token, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    private var lAngle_: std.ast.Token

    private var elementTy_: std.ast.TypeNode

    private var dollar_: std.ast.Token

    private var constant_: std.ast.Token

    private var rAngle_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop elementTy: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop dollar: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop size: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class MacroExpandExpr <: std.ast.Expr {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var at_: std.ast.Token

    private var pkgIdentifier_: std.ast.Token

    private var identifier_: std.ast.Token

    private var lSquare_: std.ast.Token

    private var macroAttrs_: std.ast.Tokens

    private var rSquare_: std.ast.Token

    private var lParen_: std.ast.Token

    private var macroInputs_: std.ast.Tokens

    private var rParen_: std.ast.Token

    private var macroInputDecl_: std.core.Option<std.ast.Decl>

    private var hasParenthesis_: Bool

    public mut prop at: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func addParen(ret: std.ast.Tokens, innerExpr: std.ast.Expr, thisExpr: std.ast.Expr, addOnEqualPred!: Bool = COMPILED_CODE /* compiled code */): std.ast.Tokens { /* compiled code */ }

let MACRO_OBJECT: std.core.ThreadLocal<std.core.CPointer<Unit>>

internal foreign func CJ_AST_ParseExpr(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>, tokenCounter: std.core.CPointer<Int64>): std.core.CPointer<std.ast.ParseRes>

internal struct ParseRes {
    internal let node: std.core.CPointer<UInt8>

    internal let errMsg: CString

    init() { /* compiled code */ }
}

internal foreign func CJ_AST_ParseDecl(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>, tokenCounter: std.core.CPointer<Int64>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParsePropMemberDecl(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParsePrimaryConstructor(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParsePattern(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>, tokenCounter: std.core.CPointer<Int64>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParseType(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>, tokenCounter: std.core.CPointer<Int64>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParseTopLevel(fptr: std.core.CPointer<Unit>, tokensBytes: std.core.CPointer<UInt8>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_ParseAnnotationArguments(tokensBytes: std.core.CPointer<UInt8>): std.core.CPointer<std.ast.ParseRes>

internal foreign func CJ_AST_Lex(fptr: std.core.CPointer<Unit>, code: CString): std.core.CPointer<UInt8>

internal foreign func CJ_CheckParentContext(fptr: std.core.CPointer<Unit>, parent: CString, report: Bool): Bool

internal foreign func CJ_SetItemInfo(fptr: std.core.CPointer<Unit>, key: CString, value: std.core.CPointer<Unit>, valueType: UInt8): Unit

internal foreign func CJ_GetChildMessages(fptr: std.core.CPointer<Unit>, children: CString): std.core.CPointer<std.core.CPointer<std.core.CPointer<Unit>>>

internal foreign func CJ_CheckAddSpace(tokBytes: std.core.CPointer<UInt8>, spaceFlag: std.core.CPointer<Bool>): Unit

internal foreign func CJ_AST_DiagReport(fptr: std.core.CPointer<Unit>, level: std.core.CPointer<Int32>, tokensBytes: std.core.CPointer<UInt8>, message: CString, hint: CString): UInt8

internal foreign func CJ_GetMacroPosition(fptr: std.core.CPointer<Unit>, fileID: std.core.CPointer<UInt32>, line: std.core.CPointer<Int32>, column: std.core.CPointer<Int32>): Unit

internal foreign func CJ_AST_Float64ToCPointer(num: Float64): CString

let BUFFER_HEADER_SIZE: Int64

internal func getInt32FromUnsafePointer(ptr: std.core.CPointer<UInt8>): Int32 { /* compiled code */ }

internal func parse(input: std.ast.Tokens, nativeParse: (std.core.CPointer<UInt8>) -> std.core.CPointer<std.ast.ParseRes>): std.ast.NodeFormat_Node { /* compiled code */ }

internal func parseFragment(input: std.ast.Tokens, startFrom: Int64, nativeParse: (std.core.CPointer<UInt8>, std.core.CPointer<Int64>) -> std.core.CPointer<std.ast.ParseRes>): (std.ast.NodeFormat_Node, Int64) { /* compiled code */ }

internal func genNodeFormatNode(pNode: std.core.CPointer<UInt8>, pTokensBytes: std.core.CPointer<UInt8>): std.ast.NodeFormat_Node { /* compiled code */ }

internal func getTokensFromBytes(pTokenArray: std.core.CPointer<UInt8>): std.ast.Tokens { /* compiled code */ }

public func cangjieLex(code: std.core.String): std.ast.Tokens { /* compiled code */ }

public func cangjieLex(code: std.core.String, truncated: Bool): std.ast.Tokens { /* compiled code */ }

internal func unsafePointerCastFromUint8Array(arr: std.core.Array<UInt8>): std.core.CPointer<UInt8> { /* compiled code */ }

public class MacroContextException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public func assertParentContext(parentMacroName: std.core.String): Unit { /* compiled code */ }

public func insideParentContext(parentMacroName: std.core.String): Bool { /* compiled code */ }

let ITEM_TYPE_STRING: UInt8

let ITEM_TYPE_INT64: UInt8

let ITEM_TYPE_BOOL: UInt8

let ITEM_TYPE_TOKENS: UInt8

public func setItem(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

public func setItem(key: std.core.String, value: Int64): Unit { /* compiled code */ }

public func setItem(key: std.core.String, value: Bool): Unit { /* compiled code */ }

public func setItem(key: std.core.String, value: std.ast.Tokens): Unit { /* compiled code */ }

internal class McInfo {
    private var keyVal: CString

    private var value_: std.core.CPointer<Unit>

    private var valueType_: UInt8

    init(keyVal_: CString, v: std.core.CPointer<Unit>, vType: UInt8) { /* compiled code */ }

    internal prop keyV: CString {
        get() { /* compiled code */ }
    }

    internal prop value: std.core.CPointer<Unit> {
        get() { /* compiled code */ }
    }

    internal prop valueType: UInt8 {
        get() { /* compiled code */ }
    }
}

public class MacroMessage {
    internal var infoMap: std.collection.ArrayList<std.ast.McInfo>

    init(res: std.collection.ArrayList<std.ast.McInfo>) { /* compiled code */ }

    public func hasItem(key: std.core.String): Bool { /* compiled code */ }

    public func getString(key: std.core.String): std.core.String { /* compiled code */ }

    public func getInt64(key: std.core.String): Int64 { /* compiled code */ }

    public func getBool(key: std.core.String): Bool { /* compiled code */ }

    public func getTokens(key: std.core.String): std.ast.Tokens { /* compiled code */ }
}

internal func readCpointerToMap(cp: std.core.CPointer<std.core.CPointer<Unit>>): std.collection.ArrayList<std.ast.McInfo> { /* compiled code */ }

public func getChildMessages(children: std.core.String): std.collection.ArrayList<std.ast.MacroMessage> { /* compiled code */ }

internal class Table {
    internal var bytes: std.core.Array<UInt8>

    internal var pos: UInt32

    init(buf: std.core.Array<UInt8>, position: UInt32) { /* compiled code */ }

    internal func offset(slot: UInt16): UInt16 { /* compiled code */ }

    internal func getIndirect(offsetVal: UInt32): UInt32 { /* compiled code */ }

    internal func getByteVector(offsetVal: UInt32): std.core.Array<UInt8> { /* compiled code */ }

    internal func getVectorLenBySlot(slot: UInt16): Int64 { /* compiled code */ }

    internal func getVectorStartBySlot(slot: UInt16): UInt32 { /* compiled code */ }

    internal func getString(offsetVal: UInt32): std.core.String { /* compiled code */ }

    internal func getUInt8(offsetVal: UInt32): UInt8 { /* compiled code */ }

    internal func getInt8(offsetVal: UInt32): Int8 { /* compiled code */ }

    internal func getBool(offsetVal: UInt32): Bool { /* compiled code */ }

    internal func getUInt16(offsetVal: UInt32): UInt16 { /* compiled code */ }

    internal func getInt16(offsetVal: UInt32): Int16 { /* compiled code */ }

    internal func getUInt32(offsetVal: UInt32): UInt32 { /* compiled code */ }

    internal func getInt32(offsetVal: UInt32): Int32 { /* compiled code */ }

    internal func getUInt64(offsetVal: UInt32): UInt64 { /* compiled code */ }

    internal func getInt64(offsetVal: UInt32): Int64 { /* compiled code */ }

    internal func getUInt8Slot(slot: UInt16, default: UInt8): UInt8 { /* compiled code */ }

    internal func getInt8Slot(slot: UInt16, default: Int8): Int8 { /* compiled code */ }

    internal func getBoolSlot(slot: UInt16, default: Bool): Bool { /* compiled code */ }

    internal func getUInt16Slot(slot: UInt16, default: UInt16): UInt16 { /* compiled code */ }

    internal func getInt16Slot(slot: UInt16, default: Int16): Int16 { /* compiled code */ }

    internal func getUInt32Slot(slot: UInt16, default: UInt32): UInt32 { /* compiled code */ }

    internal func getInt32Slot(slot: UInt16, default: Int32): Int32 { /* compiled code */ }

    internal func getUInt64Slot(slot: UInt16, default: UInt64): UInt64 { /* compiled code */ }

    internal func getInt64Slot(slot: UInt16, default: Int64): Int64 { /* compiled code */ }
}

public struct Position <: std.ast.ToBytes {
    public let fileID: UInt32

    public let line: Int32

    public let column: Int32

    init() { /* compiled code */ }

    init(fileID: UInt32, line: Int32, column: Int32) { /* compiled code */ }

    public func isEmpty(): Bool { /* compiled code */ }

    public operator func ==(r: std.ast.Position): Bool { /* compiled code */ }

    public operator func !=(r: std.ast.Position): Bool { /* compiled code */ }

    public func dump(): Unit { /* compiled code */ }

    public func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    internal func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64 { /* compiled code */ }

    internal func getSize(): Int64 { /* compiled code */ }
}

public interface ToTokens {
    public open func toTokens(): std.ast.Tokens
}

public class Program <: std.ast.Node {
    init(pkgHeader: std.ast.PackageHeader, importLists: std.collection.ArrayList<std.ast.ImportList>, decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var pkgHeader_: std.ast.PackageHeader

    private var importLists_: std.collection.ArrayList<std.ast.ImportList>

    private var decls_: std.collection.ArrayList<std.ast.Decl>

    public mut prop packageHeader: std.ast.PackageHeader {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop importLists: std.collection.ArrayList<std.ast.ImportList> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class PackageHeader <: std.ast.Node {
    init(accessibleToken: std.ast.Token, macroToken: std.ast.Token, keyword: std.ast.Token, prefixPaths: std.ast.Tokens, prefixDots: std.ast.Tokens, pkgIdentifier: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(isFromDerivation: Bool) { /* compiled code */ }

    private var accessibleToken_: std.ast.Token

    private var macroToken_: std.ast.Token

    private var keyword_: std.ast.Token

    private var prefixPaths_: std.ast.Tokens

    private var prefixDots_: std.ast.Tokens

    private var pkgIdentifier_: std.ast.Token

    private var isFromDerivation_: Bool

    public mut prop accessible: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordM: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop keywordP: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop prefixPaths: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop prefixDots: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop packageIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }

    internal func addPackageTokens(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }
}

public enum ImportKind <: std.core.ToString {
    | Single

    | Alias

    | All

    | Multi

    public func toString(): std.core.String { /* compiled code */ }
}

public class ImportContent <: std.ast.Node {
    init(importKind: std.ast.ImportKind, prefixPaths: std.ast.Tokens, prefixDots: std.ast.Tokens, identifier: std.ast.Token, importAlias: std.ast.Tokens, leftCurl: std.ast.Token, items: std.collection.ArrayList<std.ast.ImportContent>, commas: std.ast.Tokens, rightCurl: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var importKind_: std.ast.ImportKind

    private var prefixPaths_: std.ast.Tokens

    private var prefixDots_: std.ast.Tokens

    private var identifier_: std.ast.Token

    private var importAlias_: std.ast.Tokens

    private var leftCurl_: std.ast.Token

    private var items_: std.collection.ArrayList<std.ast.ImportContent>

    private var commas_: std.ast.Tokens

    private var rightCurl_: std.ast.Token

    private let innerToken_: std.ast.Tokens

    public mut prop importKind: std.ast.ImportKind {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop prefixPaths: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop prefixDots: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop importAlias: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop items: std.collection.ArrayList<std.ast.ImportContent> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func isImportAlias(): Bool { /* compiled code */ }

    public func isImportAll(): Bool { /* compiled code */ }

    public func isImportMulti(): Bool { /* compiled code */ }

    public func isImportSingle(): Bool { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend std.core.Array<T> <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

public func parseDecl(input: std.ast.Tokens, astKind!: std.core.String = COMPILED_CODE /* compiled code */): std.ast.Decl { /* compiled code */ }

public func parseDeclFragment(input: std.ast.Tokens, startFrom!: Int64 = COMPILED_CODE /* compiled code */): (std.ast.Decl, Int64) { /* compiled code */ }

internal func generateDeclAST(declNode: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Decl { /* compiled code */ }

internal func createDeclBase(keyWord: std.ast.Token, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl { /* compiled code */ }

internal func generateGenericInfo(generic: std.core.Option<std.ast.NodeFormat_Generic>): (std.core.Option<std.ast.GenericParam>, std.collection.ArrayList<std.ast.GenericConstraint>, std.ast.Tokens, Bool) { /* compiled code */ }

internal func createClassDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ClassDecl { /* compiled code */ }

internal func createStrucDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.StructDecl { /* compiled code */ }

internal func createInterfaceDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.InterfaceDecl { /* compiled code */ }

internal func createEnumDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.EnumDecl { /* compiled code */ }

internal func createExtendDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ExtendDecl { /* compiled code */ }

internal func createFuncDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl { /* compiled code */ }

internal func createFuncParam(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

internal func createFuncParam(param: std.core.Option<std.ast.NodeFormat_FuncParam>, index: Int64, length: Int64): std.ast.FuncParam { /* compiled code */ }

internal func generateFuncParams(funcBody: std.ast.NodeFormat_FuncBody): std.collection.ArrayList<std.ast.FuncParam> { /* compiled code */ }

internal func generateCommonFuncDecl(funcDecl: std.core.Option<std.ast.NodeFormat_FuncDecl>, declNode: std.ast.Decl): std.ast.FuncDecl { /* compiled code */ }

internal func createFuncBody(funcBody: std.core.Option<std.ast.NodeFormat_FuncBody>): (std.collection.ArrayList<std.ast.FuncParam>, std.ast.Token, std.core.Option<std.ast.TypeNode>, std.ast.Block, std.ast.Token, std.ast.Token) { /* compiled code */ }

internal func createMainDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MainDecl { /* compiled code */ }

internal func createMacroDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MacroDecl { /* compiled code */ }

internal func createPrimaryCtorDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.PrimaryCtorDecl { /* compiled code */ }

internal func createVarDeclNode(decl: std.core.Option<std.ast.NodeFormat_VarDecl>): std.ast.VarDecl { /* compiled code */ }

internal func createVarDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

internal func createVarWithPatternDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

internal func createPropDecl(decl: std.ast.NodeFormat_Decl): std.ast.PropDecl { /* compiled code */ }

internal func createTypeAliasDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.TypeAliasDecl { /* compiled code */ }

internal func createMacroExpandDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

public func parseExpr(input: std.ast.Tokens): std.ast.Expr { /* compiled code */ }

public func parseExprFragment(input: std.ast.Tokens, startFrom!: Int64 = COMPILED_CODE /* compiled code */): (std.ast.Expr, Int64) { /* compiled code */ }

internal func generateExprAST(exprNode: std.core.Option<std.ast.NodeFormat_Expr>): std.ast.Expr { /* compiled code */ }

internal func createBlock(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func generateBlock(blockOption: std.core.Option<std.ast.NodeFormat_Block>): std.ast.Block { /* compiled code */ }

internal func createBinaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createUnaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createIsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createAsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createParenExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createLitConstExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createCallExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createRefExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func generateRefExpr(ref: std.core.Option<std.ast.NodeFormat_RefExpr>): std.ast.RefExpr { /* compiled code */ }

internal func createReturnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createThrowExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createAssignExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createMemberAccess(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createOptionalExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createOptionalChainExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createIfExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createLetPatternExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.LetPatternExpr { /* compiled code */ }

internal func generateMatchCase(mc: std.core.Option<std.ast.NodeFormat_MatchCase>): std.ast.MatchCase { /* compiled code */ }

internal func generateMatchCaseOther(mco: std.core.Option<std.ast.NodeFormat_MatchCaseOther>): std.ast.MatchCase { /* compiled code */ }

internal func createMatchExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createDoWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createLambdaExpr(lambdaExpr: std.ast.NodeFormat_LambdaExpr): std.ast.LambdaExpr { /* compiled code */ }

internal func createLambdaExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createSpawnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createSynchronizedExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createTrailingClosureExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createTypeConvExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createForInExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createPrimitiveTypeExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createArrayLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createTupleLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createRangeExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createSubscriptExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createJumpExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createIncOrDecExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createTryExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createQuoteExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createQuoteToken(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createWildcardExpr(exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

internal func createArrayExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

internal func createMacroExpandExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

let EXPR_LIST: std.core.Array<std.core.String>

let DECL_LIST: std.core.Array<std.core.String>

let TYPE_LIST: std.core.Array<std.core.String>

let PATTERN_LIST: std.core.Array<std.core.String>

public func parseProgram(input: std.ast.Tokens): std.ast.Program { /* compiled code */ }

internal func addPositionInfo(node: std.ast.Node, nodeBase: std.ast.NodeFormat_NodeBase): Unit { /* compiled code */ }

internal func generateNode(n: std.core.Option<std.ast.NodeFormat_Node>): std.ast.Node { /* compiled code */ }

internal func createAnnotation(anno: std.core.Option<std.ast.NodeFormat_Annotation>): std.ast.Annotation { /* compiled code */ }

internal func createModifier(modifier: std.core.Option<std.ast.NodeFormat_Modifier>): std.ast.Modifier { /* compiled code */ }

internal func createArgument(arg: std.core.Option<std.ast.NodeFormat_FuncArg>): std.ast.Argument { /* compiled code */ }

internal func generateGenericParamTk(gp: std.core.Option<std.ast.NodeFormat_GenericParamDecl>): (std.ast.Token, std.ast.Token) { /* compiled code */ }

internal func createGenericParam(gic: std.core.Option<std.ast.NodeFormat_Generic>): std.core.Option<std.ast.GenericParam> { /* compiled code */ }

internal func createGenericConstraint(flag: Bool, cons: std.core.Option<std.ast.NodeFormat_GenericConstraint>): std.ast.GenericConstraint { /* compiled code */ }

internal func createMacroExpand(nodeInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>, isDecl!: Bool = COMPILED_CODE /* compiled code */, isFuncParam!: Bool = COMPILED_CODE /* compiled code */): std.ast.Node { /* compiled code */ }

internal func processCustomAnnotationLikeMacro(macroInputDecl: std.core.Option<std.ast.Decl>, macroNode: std.ast.Decl): std.ast.Decl { /* compiled code */ }

internal func getPackageIdentToken(s: std.core.String): std.ast.Token { /* compiled code */ }

internal func getPackagePrefixes(prefixPaths: std.core.Array<std.core.Option<std.core.String>>, prefixPoses: std.core.Array<std.ast.NodeFormat_Position>, prefixDotPoses: std.core.Array<std.ast.NodeFormat_Position>): (std.ast.Tokens, std.ast.Tokens) { /* compiled code */ }

internal func isSamePos(pos1: std.ast.NodeFormat_Position, pos2: std.ast.NodeFormat_Position): Bool { /* compiled code */ }

internal func createPackageHeader(pkgHeader: std.core.Option<std.ast.NodeFormat_PackageSpec>): std.ast.PackageHeader { /* compiled code */ }

internal func createImportContent(content: std.core.Option<std.ast.NodeFormat_ImportContent>): std.ast.ImportContent { /* compiled code */ }

internal func createImportList(imports: std.core.Option<std.ast.NodeFormat_ImportSpec>): std.ast.ImportList { /* compiled code */ }

internal func createProgram(file: std.core.Option<std.ast.NodeFormat_File>): std.ast.Program { /* compiled code */ }

internal func createConstructor(decl: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Constructor { /* compiled code */ }

internal func createArgumentTokens(m: std.ast.Decl): std.ast.Tokens { /* compiled code */ }

internal func createCustomAnnotation(m: std.ast.Decl): std.ast.Annotation { /* compiled code */ }

internal func createConstructorLikeMacroExpand(decl: std.ast.NodeFormat_Decl): std.ast.Constructor { /* compiled code */ }

internal func createConstructorWithAt(decl: std.ast.Decl): std.ast.Constructor { /* compiled code */ }

internal func createConstructorWithAtExcl(decl: std.ast.Decl): std.ast.Constructor { /* compiled code */ }

public func parsePattern(input: std.ast.Tokens): std.ast.Pattern { /* compiled code */ }

public func parsePatternFragment(input: std.ast.Tokens, startFrom!: Int64 = COMPILED_CODE /* compiled code */): (std.ast.Pattern, Int64) { /* compiled code */ }

internal func generatePatternAST(patternNode: std.core.Option<std.ast.NodeFormat_Pattern>): std.ast.Pattern { /* compiled code */ }

internal func createVarPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.VarPattern { /* compiled code */ }

public class VarPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var identifier_: std.ast.Token

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createConstPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ConstPattern { /* compiled code */ }

public class ConstPattern <: std.ast.Pattern {
    init(litConstExpr: std.ast.LitConstExpr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var litConstExpr_: std.ast.LitConstExpr

    public mut prop litConstExpr: std.ast.LitConstExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createWildcardPattern(_: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.WildcardPattern { /* compiled code */ }

public class WildcardPattern <: std.ast.Pattern {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyWord_: std.ast.Token

    public mut prop wildcard: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createVarOrEnumPattern(pattern: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.VarOrEnumPattern { /* compiled code */ }

public class VarOrEnumPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    private var identifier_: std.ast.Token

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createExceptTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ExceptTypePattern { /* compiled code */ }

public class ExceptTypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, bitOr: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    private var pattern_: std.ast.Pattern

    private var colon_: std.ast.Token

    private var types_: std.collection.ArrayList<std.ast.TypeNode>

    private var bitOr_: std.ast.Tokens

    public mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TypePattern { /* compiled code */ }

public class TypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    private var pattern_: std.ast.Pattern

    private var colon_: std.ast.Token

    private var type_: std.ast.TypeNode

    public mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patternType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createEnumPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.EnumPattern { /* compiled code */ }

public class EnumPattern <: std.ast.Pattern {
    init(constructor: std.ast.Expr, lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    private var constructor_: std.ast.Expr

    private var lParen_: std.ast.Token

    private var patterns_: std.collection.ArrayList<std.ast.Pattern>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    public mut prop constructor: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

internal func createTuplePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TuplePattern { /* compiled code */ }

public class TuplePattern <: std.ast.Pattern {
    init(lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var patterns_: std.collection.ArrayList<std.ast.Pattern>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public func parseType(input: std.ast.Tokens): std.ast.TypeNode { /* compiled code */ }

public func parseTypeFragment(input: std.ast.Tokens, startFrom!: Int64 = COMPILED_CODE /* compiled code */): (std.ast.TypeNode, Int64) { /* compiled code */ }

internal func generateTypeAST(tyNode: std.core.Option<std.ast.NodeFormat_Type>): std.ast.TypeNode { /* compiled code */ }

internal func createPrimitiveType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createRefType(tyNode: std.core.Option<std.ast.NodeFormat_RefType>, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createQualifiedType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createParenType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createTupleType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createThisType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createFuncType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createVArrayType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

internal func createPrefixType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

public interface ToBytes {
    public open func toBytes(): std.core.Array<UInt8>
}

internal func getMacroPosition(macroObjPtr: std.core.CPointer<Unit>): (UInt32, Int32, Int32) { /* compiled code */ }

let SIZE_OF_OFFSET: UInt32

let SIZE_OF_UINT32: UInt32

internal func castUInt32ToBytes(v: UInt32): std.core.Array<UInt8> { /* compiled code */ }

internal func castInt32ToBytes(v: Int32): std.core.Array<UInt8> { /* compiled code */ }

internal func castUInt16ToBytes(v: UInt16): std.core.Array<UInt8> { /* compiled code */ }

internal func castUInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt32): Int64 { /* compiled code */ }

internal func castInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: Int32): Int64 { /* compiled code */ }

internal func castUInt16ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt16): Int64 { /* compiled code */ }

internal func castStringToBytes(arr: std.core.Array<UInt8>, start1: Int64, s: std.core.String): Int64 { /* compiled code */ }

internal func concatBytes(l: std.core.Array<UInt8>, r: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

internal func castStringToBytes(s: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

internal func refreshPos(token: std.ast.Token, f: UInt32, l: Int32, c: Int32): std.ast.Token { /* compiled code */ }

let PATTERNS: std.core.Array<(std.ast.TokenKind, std.core.String, std.core.String)>

internal func getTokenID(tk: std.ast.TokenKind): UInt16 { /* compiled code */ }

public func getTokenKind(no: UInt16): std.ast.TokenKind { /* compiled code */ }

internal func getNewlineValue(): std.core.String { /* compiled code */ }

internal func getTokenKindValue(tk: std.ast.TokenKind): std.core.String { /* compiled code */ }

internal func getTokenLiteral(tk: std.ast.TokenKind): std.core.String { /* compiled code */ }

internal class TokenKindHelper {
    internal static let LIT_CONST_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    internal static let STRING_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    internal static let PRIMITIVE_TYPE_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    internal static func getPrimitiveTypeTokenKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    internal static func getLiteralConstKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    internal static func getStringKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    init() { /* compiled code */ }
}

public class TokensIterator <: std.core.Iterator<std.ast.Token> {
    private var position: Int64

    private let data: std.ast.Tokens

    init(tokens: std.ast.Tokens) { /* compiled code */ }

    public func peek(): std.core.Option<std.ast.Token> { /* compiled code */ }

    public func seeing(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    public func next(): std.core.Option<std.ast.Token> { /* compiled code */ }
}

internal func refreshTokensPosition(tks: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

internal func tokenToString(tk: std.ast.Token): std.core.String { /* compiled code */ }

internal class ConcatTokens <: std.ast.Tokens {
    private let leftChild: std.ast.Tokens

    private var rightChild: std.ast.Tokens

    private var cachedSize: Int64

    init(lc: std.ast.Tokens, rc: std.ast.Tokens) { /* compiled code */ }

    public override prop size: Int64 {
        get() { /* compiled code */ }
    }

    public override func get(index: Int64): std.ast.Token { /* compiled code */ }

    public operator func [](range: std.core.Range<Int64>): std.ast.Tokens { /* compiled code */ }

    public override func append(tks: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    public override func append(tk: std.ast.Token): std.ast.Tokens { /* compiled code */ }
}

extend std.ast.Token <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend std.ast.Tokens <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Int64 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Int32 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Int16 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Int8 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend UInt64 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend UInt32 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend UInt16 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend UInt8 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Float64 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Float32 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Float16 <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Bool <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Rune <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

internal func countDelimiter(charArray: std.core.Array<Rune>): (UInt16, std.core.String, std.core.String) { /* compiled code */ }

extend std.core.String <: ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }
}

public class RefType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, identifier: std.ast.Token, lAngle: std.ast.Token, typeArguments: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var identifier_: std.ast.Token

    private var lAngle_: std.ast.Token

    private var typeArguments_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rAngle_: std.ast.Token

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class QualifiedType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, baseType: std.ast.TypeNode, dot: std.ast.Token, identifier: std.ast.Token, lAngle: std.ast.Token, typeArguments: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var baseType_: std.ast.TypeNode

    private var dot_: std.ast.Token

    private var identifier_: std.ast.Token

    private var lAngle_: std.ast.Token

    private var typeArguments_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rAngle_: std.ast.Token

    public mut prop baseType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop dot: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ParenType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, parenthesizedType: std.ast.TypeNode, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var ty_: std.ast.TypeNode

    private var rParen_: std.ast.Token

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop parenthesizedType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class TupleType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var lParen_: std.ast.Token

    private var types_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class ThisType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class PrefixType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, prefixOps: std.ast.Tokens, baseType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var prefixOps_: std.ast.Tokens

    private var baseType_: std.ast.TypeNode

    public mut prop prefixOps: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop baseType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

public class FuncType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token, lParen: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token, arrow: std.ast.Token, returnType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    private var keyword_: std.ast.Token

    private var lParen_: std.ast.Token

    private var types_: std.collection.ArrayList<std.ast.TypeNode>

    private var commas_: std.ast.Tokens

    private var rParen_: std.ast.Token

    private var arrow_: std.ast.Token

    private var retTy_: std.ast.TypeNode

    public mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop arrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop returnType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    protected func dump(indent: UInt16): std.core.String { /* compiled code */ }
}
