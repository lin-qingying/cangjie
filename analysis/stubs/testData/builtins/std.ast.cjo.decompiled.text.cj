// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.ast

import std.core.*
import std.collection.ArrayList

let NUM_SPACES: UInt16

enum NodeFormat_AnyExpr {
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

func EnumValuesNodeFormat_AnyExpr(e: UInt8): std.ast.NodeFormat_AnyExpr { /* compiled code */ }

enum NodeFormat_AnyPattern {
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

func EnumValuesNodeFormat_AnyPattern(e: UInt8): std.ast.NodeFormat_AnyPattern { /* compiled code */ }

enum NodeFormat_MacroParam {
    | MacroParam_NONE

    | MacroParam_MACRO_EXPAND_PARAM
}

func EnumValuesNodeFormat_MacroParam(e: UInt8): std.ast.NodeFormat_MacroParam { /* compiled code */ }

enum NodeFormat_AnyType {
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

func EnumValuesNodeFormat_AnyType(e: UInt8): std.ast.NodeFormat_AnyType { /* compiled code */ }

enum NodeFormat_AnyDecl {
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

func EnumValuesNodeFormat_AnyDecl(e: UInt8): std.ast.NodeFormat_AnyDecl { /* compiled code */ }

enum NodeFormat_AnyNode {
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

func EnumValuesNodeFormat_AnyNode(e: UInt8): std.ast.NodeFormat_AnyNode { /* compiled code */ }

enum NodeFormat_AccessibleKind {
    | AccessibleKind_ACCESSIBLE_PUBLIC

    | AccessibleKind_ACCESSIBLE_PROTECTED

    | AccessibleKind_ACCESSIBLE_INTERNAL
}

func EnumValuesNodeFormat_AccessibleKind(e: UInt8): std.ast.NodeFormat_AccessibleKind { /* compiled code */ }

enum NodeFormat_ImportKind {
    | ImportKind_IMPORT_SINGLE

    | ImportKind_IMPORT_ALIAS

    | ImportKind_IMPORT_ALL

    | ImportKind_IMPORT_MULTI
}

func EnumValuesNodeFormat_ImportKind(e: UInt8): std.ast.NodeFormat_ImportKind { /* compiled code */ }

enum NodeFormat_ReExportKind {
    | ReExportKind_REEXPORT_PRIVATE

    | ReExportKind_REEXPORT_INTERNAL

    | ReExportKind_REEXPORT_PROTECTED

    | ReExportKind_REEXPORT_PUBLIC
}

func EnumValuesNodeFormat_ReExportKind(e: UInt8): std.ast.NodeFormat_ReExportKind { /* compiled code */ }

struct NodeFormat_Position {
    let BYTE_ALIGNMENT: UInt32

    let fileId: UInt32

    let line: Int32

    let column: Int32

    init() { /* compiled code */ }

    init(buf: std.core.Array<UInt8>, pos: UInt32) { /* compiled code */ }
}

class NodeFormat_MatchCase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERNS: UInt16

    let PATTERNGUARD: UInt16

    let IF_POS: UInt16

    let ARROW_POS: UInt16

    let EXPR_OR_DECLS: UInt16

    let BIT_OR_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    func GetPatternguard(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExprOrDecls(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_NodeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BEGIN: UInt16

    let END: UInt16

    let AST_KIND: UInt16

    func GetBegin(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetEnd(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAstKind(): std.core.String { /* compiled code */ }
}

class NodeFormat_Pattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN_TYPE: UInt16

    let PATTERN: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPatternType(): std.ast.NodeFormat_AnyPattern { /* compiled code */ }

    func GetPatternAsConstPattern(): std.core.Option<std.ast.NodeFormat_ConstPattern> { /* compiled code */ }

    func GetPatternAsWildcardPattern(): std.core.Option<std.ast.NodeFormat_WildcardPattern> { /* compiled code */ }

    func GetPatternAsVarPattern(): std.core.Option<std.ast.NodeFormat_VarPattern> { /* compiled code */ }

    func GetPatternAsExceptTypePattern(): std.core.Option<std.ast.NodeFormat_ExceptTypePattern> { /* compiled code */ }

    func GetPatternAsTypePattern(): std.core.Option<std.ast.NodeFormat_TypePattern> { /* compiled code */ }

    func GetPatternAsEnumPattern(): std.core.Option<std.ast.NodeFormat_EnumPattern> { /* compiled code */ }

    func GetPatternAsVarOrEnumPattern(): std.core.Option<std.ast.NodeFormat_VarOrEnumPattern> { /* compiled code */ }

    func GetPatternAsTuplePattern(): std.core.Option<std.ast.NodeFormat_TuplePattern> { /* compiled code */ }

    func GetPatternAsCommandTypePattern(): std.core.Option<std.ast.NodeFormat_CommandTypePattern> { /* compiled code */ }
}

class NodeFormat_Expr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR_TYPE: UInt16

    let EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExprType(): std.ast.NodeFormat_AnyExpr { /* compiled code */ }

    func GetExprAsBinaryExpr(): std.core.Option<std.ast.NodeFormat_BinaryExpr> { /* compiled code */ }

    func GetExprAsUnaryExpr(): std.core.Option<std.ast.NodeFormat_UnaryExpr> { /* compiled code */ }

    func GetExprAsParenExpr(): std.core.Option<std.ast.NodeFormat_ParenExpr> { /* compiled code */ }

    func GetExprAsLitConstExpr(): std.core.Option<std.ast.NodeFormat_LitConstExpr> { /* compiled code */ }

    func GetExprAsCallExpr(): std.core.Option<std.ast.NodeFormat_CallExpr> { /* compiled code */ }

    func GetExprAsRefExpr(): std.core.Option<std.ast.NodeFormat_RefExpr> { /* compiled code */ }

    func GetExprAsReturnExpr(): std.core.Option<std.ast.NodeFormat_ReturnExpr> { /* compiled code */ }

    func GetExprAsAssignExpr(): std.core.Option<std.ast.NodeFormat_AssignExpr> { /* compiled code */ }

    func GetExprAsMemberAccess(): std.core.Option<std.ast.NodeFormat_MemberAccess> { /* compiled code */ }

    func GetExprAsIfExpr(): std.core.Option<std.ast.NodeFormat_IfExpr> { /* compiled code */ }

    func GetExprAsBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetExprAsLambdaExpr(): std.core.Option<std.ast.NodeFormat_LambdaExpr> { /* compiled code */ }

    func GetExprAsArrayLit(): std.core.Option<std.ast.NodeFormat_ArrayLit> { /* compiled code */ }

    func GetExprAsTupleLit(): std.core.Option<std.ast.NodeFormat_TupleLit> { /* compiled code */ }

    func GetExprAsSubscriptExpr(): std.core.Option<std.ast.NodeFormat_SubscriptExpr> { /* compiled code */ }

    func GetExprAsRangeExpr(): std.core.Option<std.ast.NodeFormat_RangeExpr> { /* compiled code */ }

    func GetExprAsForInExpr(): std.core.Option<std.ast.NodeFormat_ForInExpr> { /* compiled code */ }

    func GetExprAsIsExpr(): std.core.Option<std.ast.NodeFormat_IsExpr> { /* compiled code */ }

    func GetExprAsAsExpr(): std.core.Option<std.ast.NodeFormat_AsExpr> { /* compiled code */ }

    func GetExprAsTrailingClosureExpr(): std.core.Option<std.ast.NodeFormat_TrailingClosureExpr> { /* compiled code */ }

    func GetExprAsTypeConvExpr(): std.core.Option<std.ast.NodeFormat_TypeConvExpr> { /* compiled code */ }

    func GetExprAsThrowExpr(): std.core.Option<std.ast.NodeFormat_ThrowExpr> { /* compiled code */ }

    func GetExprAsTryExpr(): std.core.Option<std.ast.NodeFormat_TryExpr> { /* compiled code */ }

    func GetExprAsPrimitiveTypeExpr(): std.core.Option<std.ast.NodeFormat_PrimitiveTypeExpr> { /* compiled code */ }

    func GetExprAsWhileExpr(): std.core.Option<std.ast.NodeFormat_WhileExpr> { /* compiled code */ }

    func GetExprAsDoWhileExpr(): std.core.Option<std.ast.NodeFormat_DoWhileExpr> { /* compiled code */ }

    func GetExprAsJumpExpr(): std.core.Option<std.ast.NodeFormat_JumpExpr> { /* compiled code */ }

    func GetExprAsIncOrDecExpr(): std.core.Option<std.ast.NodeFormat_IncOrDecExpr> { /* compiled code */ }

    func GetExprAsSpawnExpr(): std.core.Option<std.ast.NodeFormat_SpawnExpr> { /* compiled code */ }

    func GetExprAsSynchronizedExpr(): std.core.Option<std.ast.NodeFormat_SynchronizedExpr> { /* compiled code */ }

    func GetExprAsLetPatternDestructor(): std.core.Option<std.ast.NodeFormat_LetPatternDestructor> { /* compiled code */ }

    func GetExprAsTokenPart(): std.core.Option<std.ast.NodeFormat_TokenPart> { /* compiled code */ }

    func GetExprAsQuoteExpr(): std.core.Option<std.ast.NodeFormat_QuoteExpr> { /* compiled code */ }

    func GetExprAsMatchExpr(): std.core.Option<std.ast.NodeFormat_MatchExpr> { /* compiled code */ }

    func GetExprAsOptionalExpr(): std.core.Option<std.ast.NodeFormat_OptionalExpr> { /* compiled code */ }

    func GetExprAsOptionalChainExpr(): std.core.Option<std.ast.NodeFormat_OptionalChainExpr> { /* compiled code */ }

    func GetExprAsMacroExpandExpr(): std.core.Option<std.ast.NodeFormat_MacroExpandExpr> { /* compiled code */ }

    func GetExprAsWildcardExpr(): std.core.Option<std.ast.NodeFormat_WildcardExpr> { /* compiled code */ }

    func GetExprAsArrayExpr(): std.core.Option<std.ast.NodeFormat_ArrayExpr> { /* compiled code */ }
}

class NodeFormat_Block <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let BODY: UInt16

    let RIGHT_CURL_POS: UInt16

    let IS_UNSAFE: UInt16

    let UNSAFE_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetBody(): std.core.Array<std.core.Option<std.ast.NodeFormat_Node>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsUnsafe(): Bool { /* compiled code */ }

    func GetUnsafePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class FlatBufferObject {
    let table: std.ast.Table

    init(buf: std.core.Array<UInt8>, root: UInt32) { /* compiled code */ }
}

class NodeFormat_MatchCaseOther <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let MATCH_EXPR: UInt16

    let ARROW_POS: UInt16

    let EXPR_OR_DECLS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetMatchExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExprOrDecls(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

class NodeFormat_ConstPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LITERAL: UInt16

    let OPERATOR_CALL_EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLiteral(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetOperatorCallExpr(): std.core.Option<std.ast.NodeFormat_CallExpr> { /* compiled code */ }
}

class NodeFormat_WildcardPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }
}

class NodeFormat_VarPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let VAR_DECL: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetVarDecl(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }
}

class NodeFormat_ExceptTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let PATTERN_POS: UInt16

    let COLON_POS: UInt16

    let TYPES: UInt16

    let BIT_OR_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetPatternPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_TypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let COLON_POS: UInt16

    let TYPE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }
}

class NodeFormat_EnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REF: UInt16

    let LEFT_PAREN_POS: UInt16

    let PATTERNS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetRef(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_VarOrEnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IDENTIFIER: UInt16

    let PATTERN: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }
}

class NodeFormat_TuplePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PATTERNS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_CommandTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let PATTERN_POS: UInt16

    let COLON_POS: UInt16

    let TYPES: UInt16

    let BIT_OR_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetPatternPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_CallExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_FUNC: UInt16

    let LEFT_PAREN_POS: UInt16

    let ARGS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBaseFunc(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_VarDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let COLON_POS: UInt16

    let INITIALIZER: UInt16

    let ASSIGN_POS: UInt16

    let IS_VAR: UInt16

    let ISENUMCONSTRUCT: UInt16

    let EMPTYKEYWORD: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsVar(): Bool { /* compiled code */ }

    func GetIsEnumConstruct(): Bool { /* compiled code */ }

    func GetEmptyKeyword(): Bool { /* compiled code */ }
}

class NodeFormat_Type <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE_TYPE: UInt16

    let TYPE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetTypeType(): std.ast.NodeFormat_AnyType { /* compiled code */ }

    func GetTypeAsRefType(): std.core.Option<std.ast.NodeFormat_RefType> { /* compiled code */ }

    func GetTypeAsPrimitiveType(): std.core.Option<std.ast.NodeFormat_PrimitiveType> { /* compiled code */ }

    func GetTypeAsFuncType(): std.core.Option<std.ast.NodeFormat_FuncType> { /* compiled code */ }

    func GetTypeAsThisType(): std.core.Option<std.ast.NodeFormat_ThisType> { /* compiled code */ }

    func GetTypeAsParenType(): std.core.Option<std.ast.NodeFormat_ParenType> { /* compiled code */ }

    func GetTypeAsQualifiedType(): std.core.Option<std.ast.NodeFormat_QualifiedType> { /* compiled code */ }

    func GetTypeAsOptionType(): std.core.Option<std.ast.NodeFormat_OptionType> { /* compiled code */ }

    func GetTypeAsTupleType(): std.core.Option<std.ast.NodeFormat_TupleType> { /* compiled code */ }

    func GetTypeAsVArrayType(): std.core.Option<std.ast.NodeFormat_VArrayType> { /* compiled code */ }

    func GetTypeAsConstantType(): std.core.Option<std.ast.NodeFormat_ConstantType> { /* compiled code */ }
}

class NodeFormat_BinaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_EXPR: UInt16

    let RIGHT_EXPR: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetOperatorKind(): UInt16 { /* compiled code */ }

    func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_IsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let IS_TYPE: UInt16

    let IS_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetIsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetIsPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_AsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let AS_TYPE: UInt16

    let AS_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetAsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetAsPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_UnaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetOperatorKind(): UInt16 { /* compiled code */ }

    func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_WildcardExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }
}

class NodeFormat_ArrayExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let LEFTPARENPOS: UInt16

    let ARGS: UInt16

    let RIGHTPARENPOS: UInt16

    let ISVALUEARRAY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsValueArray(): Bool { /* compiled code */ }
}

class NodeFormat_FuncArg <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let NAME: UInt16

    let NAME_POS: UInt16

    let COLON_POS: UInt16

    let EXPR: UInt16

    let COMMA_POS: UInt16

    let WITHINOUT: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetName(): std.core.String { /* compiled code */ }

    func GetNamePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetWithInout(): Bool { /* compiled code */ }
}

class NodeFormat_ParenExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_LitConstExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LITERAL: UInt16

    let LITERAL_CONST_KIND: UInt16

    let DELIMITER_NUM: UInt16

    let STRING_KIND: UInt16

    let IS_SINGLE_QUOTE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLiteral(): std.core.String { /* compiled code */ }

    func GetLiteralConstKind(): UInt16 { /* compiled code */ }

    func GetDelimiterNum(): UInt16 { /* compiled code */ }

    func GetStringKind(): UInt16 { /* compiled code */ }

    func GetIsSingleQuote(): Bool { /* compiled code */ }
}

class NodeFormat_RefExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REF: UInt16

    let LEFT_ANGLE_POS: UInt16

    let TYPE_ARGUMENTS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    let IS_THIS: UInt16

    let IS_SUPER: UInt16

    let IS_QUOTE_DOLLAR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetRef(): std.core.Option<std.ast.NodeFormat_Reference> { /* compiled code */ }

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsThis(): Bool { /* compiled code */ }

    func GetIsSuper(): Bool { /* compiled code */ }

    func GetIsQuoteDollar(): Bool { /* compiled code */ }
}

class NodeFormat_Reference <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let IDENTIFIER: UInt16

    let IDENTIFIER_POS: UInt16

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_ReturnExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let RETURN_POS: UInt16

    let EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetReturnPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_AssignExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_VALUE: UInt16

    let ASSIGN_OP: UInt16

    let ASSIGN_POS: UInt16

    let RIGHT_EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftValue(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetAssignOp(): UInt16 { /* compiled code */ }

    func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_MemberAccess <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_EXPR: UInt16

    let DOT_POS: UInt16

    let FIELD: UInt16

    let FIELD_POS: UInt16

    let LEFT_ANGLE_POS: UInt16

    let TYPE_ARGUMENTS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetDotPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetField(): std.core.String { /* compiled code */ }

    func GetFieldPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_IfExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IF_POS: UInt16

    let COND_EXPR: UInt16

    let BODY: UInt16

    let HAS_ELSE: UInt16

    let ELSE_POS: UInt16

    let ELSE_BODY: UInt16

    let ISELSEIF: UInt16

    let LEFT_PAREN_POS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetHasElse(): Bool { /* compiled code */ }

    func GetElsePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetElseBody(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetIsElseIf(): Bool { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_LetPatternDestructor <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERNS: UInt16

    let BIT_OR_POS_VEC: UInt16

    let BACKARROW_POS: UInt16

    let INITIALIZER: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetBackarrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_LambdaExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BODY: UInt16

    let MOCKSUPPORTED: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }

    func GetMockSupported(): Bool { /* compiled code */ }
}

class NodeFormat_FuncBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PARAM_LIST: UInt16

    let ARROW_POS: UInt16

    let COLON_POS: UInt16

    let RET_TYPE: UInt16

    let HAS_BODY: UInt16

    let BODY: UInt16

    let GENERIC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetParamList(): std.core.Option<std.ast.NodeFormat_FuncParamList> { /* compiled code */ }

    func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetHasBody(): Bool { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

class NodeFormat_ArrayLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let CHILDREN: UInt16

    let COMMA_POS_VEC: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetChildren(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_TupleLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let CHILDREN: UInt16

    let COMMA_POS_VEC: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetChildren(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    func GetCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_SubscriptExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_EXPR: UInt16

    let LEFT_SQUARE_POS: UInt16

    let INDEX_EXPRS: UInt16

    let RIGHT_SQUARE_POS: UInt16

    let IS_TUPLE_ACCESS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIndexExprs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsTupleAccess(): Bool { /* compiled code */ }
}

class NodeFormat_MatchExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let MATCH_MODE: UInt16

    let LEFT_PAREN_POS: UInt16

    let SELECTOR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let LEFT_CURL_POS: UInt16

    let MATCH_CASES: UInt16

    let MATCH_CASE_OTHERS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetMatchMode(): Bool { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetSelector(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetMatchCases(): std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCase>> { /* compiled code */ }

    func GetMatchCaseOthers(): std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCaseOther>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_RangeExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let START_EXPR: UInt16

    let RANGE_POS: UInt16

    let STOP_EXPR: UInt16

    let COLON_POS: UInt16

    let STEP_EXPR: UInt16

    let IS_CLOSED: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetStartExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRangePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetStopExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetStepExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetIsClosed(): Bool { /* compiled code */ }
}

class NodeFormat_ForInExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PATTERN: UInt16

    let IN_POS: UInt16

    let IN_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let IF_POS: UInt16

    let PATTERN_GUARD: UInt16

    let BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetInPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetInExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIfPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPatternGuard(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

class NodeFormat_WhileExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let WHILE_POS: UInt16

    let LEFT_PAREN_POS: UInt16

    let COND_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetWhilePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

class NodeFormat_SpawnExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let SPAWN_POS: UInt16

    let TASK_EXPR: UInt16

    let HAS_ARG: UInt16

    let SPAWN_ARG_EXPR: UInt16

    let LEFT_PAREN_POS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetSpawnPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTaskExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetHasArg(): Bool { /* compiled code */ }

    func GetSpawnArgExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_SynchronizedExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let SYNC_POS: UInt16

    let LEFT_PAREN_POS: UInt16

    let MUTEX_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetSyncPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetMutexExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }
}

class NodeFormat_TrailingClosureExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_LAMBDA: UInt16

    let EXPR: UInt16

    let LAMBDA: UInt16

    let RIGHT_LAMBDA: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftLambda(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLambda(): std.core.Option<std.ast.NodeFormat_LambdaExpr> { /* compiled code */ }

    func GetRightLambda(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_TypeConvExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_ThrowExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_TryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let RESOURCE_SPEC: UInt16

    let IS_RESOURCE_SPEC: UInt16

    let TRY_BLOCK: UInt16

    let CATCH_BLOCKS: UInt16

    let CATCH_PATTERNS: UInt16

    let FINALLY_POS: UInt16

    let FINALLY_BLOCK: UInt16

    let RESOURCE_SPEC_LPAREN_POS: UInt16

    let RESOURCE_SPEC_RPAREN_POS: UInt16

    let RESOURCE_SPEC_COMMA_POS_VEC: UInt16

    let CATCH_POS_VEC: UInt16

    let CATCH_LEFT_PAREN_POS_VEC: UInt16

    let CATCH_RIGHT_PAREN_POS_VEC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetResourceSpec(): std.core.Array<std.core.Option<std.ast.NodeFormat_VarDecl>> { /* compiled code */ }

    func GetIsResourceSpec(): Bool { /* compiled code */ }

    func GetTryBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetCatchBlocks(): std.core.Array<std.core.Option<std.ast.NodeFormat_Block>> { /* compiled code */ }

    func GetCatchPatterns(): std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>> { /* compiled code */ }

    func GetFinallyPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetFinallyBlock(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetResourceSpecLparenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetResourceSpecRparenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetResourceSpecCommaPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetCatchPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetCatchLeftParenPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetCatchRightParenPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_DoWhileExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let DO_POS: UInt16

    let BODY: UInt16

    let WHILE_POS: UInt16

    let LEFT_PAREN_POS: UInt16

    let COND_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetDoPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_Block> { /* compiled code */ }

    func GetWhilePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_IncOrDecExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    let EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetOperatorKind(): UInt16 { /* compiled code */ }

    func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_OptionalExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASEEXPR: UInt16

    let QUEST_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBaseExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetQuestPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_OptionalChainExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }
}

class NodeFormat_Token <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let KIND: UInt16

    let VALUE: UInt16

    let POS: UInt16

    func GetKind(): UInt16 { /* compiled code */ }

    func GetValue(): std.core.String { /* compiled code */ }

    func GetPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_TokenPart <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let TOKENS: UInt16

    func GetTokens(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }
}

class NodeFormat_QuoteExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPRS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetExprs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_FuncParamList <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PARAMS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetParams(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncParam>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_FuncParam <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let NODEBASE: UInt16

    let BASE: UInt16

    let COLON_POS: UInt16

    let ASSIGNMENT: UInt16

    let COMMA_POS: UInt16

    let IS_NAMED_PARAM: UInt16

    let IS_MEMBER_PARAM: UInt16

    let NOT_MARK_POS: UInt16

    let HAS_LET_OR_VAR: UInt16

    let MACROPARAM_TYPE: UInt16

    let MACROPARAM: UInt16

    func GetNodeBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetBase(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAssignment(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsNamedParam(): Bool { /* compiled code */ }

    func GetIsMemberParam(): Bool { /* compiled code */ }

    func GetNotMarkPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetHasLetOrVar(): Bool { /* compiled code */ }

    func GetMacroParamType(): std.ast.NodeFormat_MacroParam { /* compiled code */ }

    func GetMacroParamAsMacroExpandParam(): std.core.Option<std.ast.NodeFormat_MacroExpandParam> { /* compiled code */ }
}

class NodeFormat_MacroExpandParam <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_FuncParam> { /* compiled code */ }

    func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }
}

class NodeFormat_Generic <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let LEFT_ANGLE_POS: UInt16

    let TYPE_PARAMETERS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    let GENERIC_CONSTRAINTS: UInt16

    let CONTENT: UInt16

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeParameters(): std.core.Array<std.core.Option<std.ast.NodeFormat_GenericParamDecl>> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetGenericConstraints(): std.core.Array<std.core.Option<std.ast.NodeFormat_GenericConstraint>> { /* compiled code */ }

    func GetContent(): std.core.String { /* compiled code */ }
}

class NodeFormat_JumpExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IS_BREAK: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetIsBreak(): Bool { /* compiled code */ }
}

class NodeFormat_PrimitiveTypeExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetKind(): UInt16 { /* compiled code */ }
}

class NodeFormat_MacroExpandExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    let IDENTIFIER: UInt16

    let IDENTIFIERPOS: UInt16

    let ANNOTATIONS: UInt16

    let MODIFIERS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAnnotations(): std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>> { /* compiled code */ }

    func GetModifiers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>> { /* compiled code */ }
}

class NodeFormat_TypeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMMA_POS: UInt16

    let TYPE_PARAMETER_NAME: UInt16

    let COLON_POS: UInt16

    let TYPE_POS: UInt16

    let BIT_AND_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeParameterName(): std.core.String { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetBitAndPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_RefType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REF: UInt16

    let LEFT_ANGLE_POS: UInt16

    let TYPE_ARGUMENTS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetRef(): std.core.Option<std.ast.NodeFormat_Reference> { /* compiled code */ }

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_PrimitiveType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE_STR: UInt16

    let KIND: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetTypeStr(): std.core.String { /* compiled code */ }

    func GetKind(): UInt16 { /* compiled code */ }
}

class NodeFormat_FuncType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PARAM_TYPES: UInt16

    let RIGHT_PAREN_POS: UInt16

    let ARROW_POS: UInt16

    let RET_TYPE: UInt16

    let ISC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetParamTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetArrowPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetIsC(): Bool { /* compiled code */ }
}

class NodeFormat_ThisType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }
}

class NodeFormat_ParenType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let TYPE: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_QualifiedType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_TYPE: UInt16

    let DOT_POS: UInt16

    let FIELD: UInt16

    let FIELD_POS: UInt16

    let LEFT_ANGLE_POS: UInt16

    let TYPE_ARGUMENTS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetBaseType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetDotPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetField(): std.core.String { /* compiled code */ }

    func GetFieldPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeArguments(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_OptionType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMPONENT_TYPE: UInt16

    let QUEST_NUM: UInt16

    let QUEST_VECTOR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetComponentType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetQuestNum(): Int32 { /* compiled code */ }

    func GetQuestVector(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_TupleType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FIELD_TYPES: UInt16

    let LEFT_PAREN_POS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VECTOR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetFieldTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetCommaPosVector(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }
}

class NodeFormat_VArrayType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let VARRAYPOS: UInt16

    let LEFTANGLEPOS: UInt16

    let TYPEARGUMENT: UInt16

    let CONSTANTTYPE: UInt16

    let RIGHTANGLEPOS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetVarrayPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetTypeArgument(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetConstantType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetRightAnglePos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_ConstantType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let CONSTANTEXPR: UInt16

    let DOLLARPOS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_TypeBase> { /* compiled code */ }

    func GetConstantExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetDollarPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_DeclBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IDENTIFIER: UInt16

    let IDENTIFIER_POS: UInt16

    let KEYWORD_POS: UInt16

    let ANNOTATIONS: UInt16

    let MODIFIERS: UInt16

    let GENERIC: UInt16

    let ISCONST: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetKeywordPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAnnotations(): std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>> { /* compiled code */ }

    func GetModifiers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>> { /* compiled code */ }

    func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }

    func GetIsConst(): Bool { /* compiled code */ }
}

class NodeFormat_VarWithPatternDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let COLON_POS: UInt16

    let INITIALIZER: UInt16

    let PATTERN: UInt16

    let ASSIGN_POS: UInt16

    let IS_VAR: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetInitializer(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsVar(): Bool { /* compiled code */ }
}

class NodeFormat_FuncDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let FUNC_BODY: UInt16

    let IS_SETTER: UInt16

    let IS_GETTER: UInt16

    let OP_KIND: UInt16

    let ISENUMCONSTRUCT: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }

    func GetIsSetter(): Bool { /* compiled code */ }

    func GetIsGetter(): Bool { /* compiled code */ }

    func GetOpKind(): Int32 { /* compiled code */ }

    func GetIsEnumConstruct(): Bool { /* compiled code */ }
}

class NodeFormat_MainDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FUNC_BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

class NodeFormat_MacroDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFTPARENPOS: UInt16

    let RIGHTPARENPOS: UInt16

    let FUNC_BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

class NodeFormat_StructBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_Decl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let DECL_TYPE: UInt16

    let DECL: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetDeclType(): std.ast.NodeFormat_AnyDecl { /* compiled code */ }

    func GetDeclAsFuncDecl(): std.core.Option<std.ast.NodeFormat_FuncDecl> { /* compiled code */ }

    func GetDeclAsMainDecl(): std.core.Option<std.ast.NodeFormat_MainDecl> { /* compiled code */ }

    func GetDeclAsVarDecl(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    func GetDeclAsVarWithPatternDecl(): std.core.Option<std.ast.NodeFormat_VarWithPatternDecl> { /* compiled code */ }

    func GetDeclAsStructDecl(): std.core.Option<std.ast.NodeFormat_StructDecl> { /* compiled code */ }

    func GetDeclAsClassDecl(): std.core.Option<std.ast.NodeFormat_ClassDecl> { /* compiled code */ }

    func GetDeclAsInterfaceDecl(): std.core.Option<std.ast.NodeFormat_InterfaceDecl> { /* compiled code */ }

    func GetDeclAsGenericParamDecl(): std.core.Option<std.ast.NodeFormat_GenericParamDecl> { /* compiled code */ }

    func GetDeclAsPrimaryCtorDecl(): std.core.Option<std.ast.NodeFormat_PrimaryCtorDecl> { /* compiled code */ }

    func GetDeclAsPropDecl(): std.core.Option<std.ast.NodeFormat_PropDecl> { /* compiled code */ }

    func GetDeclAsEnumDecl(): std.core.Option<std.ast.NodeFormat_EnumDecl> { /* compiled code */ }

    func GetDeclAsTypeAliasDecl(): std.core.Option<std.ast.NodeFormat_TypeAliasDecl> { /* compiled code */ }

    func GetDeclAsExtendDecl(): std.core.Option<std.ast.NodeFormat_ExtendDecl> { /* compiled code */ }

    func GetDeclAsMacroDecl(): std.core.Option<std.ast.NodeFormat_MacroDecl> { /* compiled code */ }

    func GetDeclAsMacroExpandDecl(): std.core.Option<std.ast.NodeFormat_MacroExpandDecl> { /* compiled code */ }

    func GetDeclAsFuncParam(): std.core.Option<std.ast.NodeFormat_FuncParam> { /* compiled code */ }
}

class NodeFormat_StructDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BODY: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let GENERIC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_StructBody> { /* compiled code */ }

    func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

class NodeFormat_ClassBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_ClassDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let BODY: UInt16

    let SUB_DECLS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_ClassBody> { /* compiled code */ }

    func GetSubDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

class NodeFormat_InterfaceBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_InterfaceDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let BODY: UInt16

    let SUB_DECLS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetSuperTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetBody(): std.core.Option<std.ast.NodeFormat_InterfaceBody> { /* compiled code */ }

    func GetSubDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

class NodeFormat_EnumDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let HAS_ARGUMENTS: UInt16

    let LEFT_CURL_POS: UInt16

    let CONSTRUCTORS: UInt16

    let BIT_OR_POS_VEC: UInt16

    let MEMBERS: UInt16

    let RIGHT_CURL_POS: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_INTERFACE_TYPES: UInt16

    let HASELLIPSIS: UInt16

    let ELLIPSIS_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetHasArguments(): Bool { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetConstructors(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetBitOrPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetMembers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetSuperInterfaceTypes(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetHasEllipsis(): Bool { /* compiled code */ }

    func GetEllipsisPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_GenericParamDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMMA_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_PrimaryCtorDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FUNC_BODY: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetFuncBody(): std.core.Option<std.ast.NodeFormat_FuncBody> { /* compiled code */ }
}

class NodeFormat_PropDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COLON_POS: UInt16

    let LEFT_CURL_POS: UInt16

    let GETTERS: UInt16

    let SETTERS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_VarDecl> { /* compiled code */ }

    func GetColonPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetGetters(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>> { /* compiled code */ }

    func GetSetters(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_GenericConstraint <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let WHERE_POS: UInt16

    let TYPE: UInt16

    let OPERATOR_POS: UInt16

    let UPPER_BOUND: UInt16

    let BIT_AND_POS_VEC: UInt16

    let COMMA_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetWherePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_RefType> { /* compiled code */ }

    func GetOperatorPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetUpperBound(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetBitAndPosVec(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetCommaPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_Annotation <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    let IDENTPOS: UInt16

    let IDENTIFIER: UInt16

    let ARGS: UInt16

    let OVERFLOW_STRATEGY: UInt16

    let ATTRS: UInt16

    let CONDEXPR: UInt16

    let LEFT_SQUARE_POS: UInt16

    let RIGHT_SQUARE_POS: UInt16

    let IS_COMPILE_TIME_VISIBLE: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetKind(): UInt16 { /* compiled code */ }

    func GetIdentPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>> { /* compiled code */ }

    func GetOverflowStrategy(): std.core.String { /* compiled code */ }

    func GetAttrs(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    func GetCondExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetIsCompileTimeVisible(): Bool { /* compiled code */ }
}

class NodeFormat_Modifier <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    let IS_EXPLICIT: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetKind(): UInt16 { /* compiled code */ }

    func GetIsExplicit(): Bool { /* compiled code */ }
}

class NodeFormat_TypeAliasDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let ASSIGN_POS: UInt16

    let TYPE: UInt16

    let GENERIC: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetAssignPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetGeneric(): std.core.Option<std.ast.NodeFormat_Generic> { /* compiled code */ }
}

class NodeFormat_ExtendDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXTENDED_TYPE: UInt16

    let UPPER_BOUND_POS: UInt16

    let INTERFACES: UInt16

    let WHERE_POS: UInt16

    let LEFT_CURL_POS: UInt16

    let MEMBERS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetExtendedType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetUpperBoundPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetInterfaces(): std.core.Array<std.core.Option<std.ast.NodeFormat_Type>> { /* compiled code */ }

    func GetWherePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetMembers(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

class NodeFormat_MacroExpandDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_DeclBase> { /* compiled code */ }

    func GetInvocation(): std.core.Option<std.ast.NodeFormat_MacroInvocation> { /* compiled code */ }
}

class NodeFormat_Node <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let ROOT_TYPE: UInt16

    let ROOT: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetRootType(): std.ast.NodeFormat_AnyNode { /* compiled code */ }

    func GetRootAsExpr(): std.core.Option<std.ast.NodeFormat_Expr> { /* compiled code */ }

    func GetRootAsDecl(): std.core.Option<std.ast.NodeFormat_Decl> { /* compiled code */ }

    func GetRootAsType(): std.core.Option<std.ast.NodeFormat_Type> { /* compiled code */ }

    func GetRootAsPattern(): std.core.Option<std.ast.NodeFormat_Pattern> { /* compiled code */ }

    func GetRootAsFile(): std.core.Option<std.ast.NodeFormat_File> { /* compiled code */ }

    func GetRootAsPackageSpec(): std.core.Option<std.ast.NodeFormat_PackageSpec> { /* compiled code */ }

    func GetRootAsImportSpec(): std.core.Option<std.ast.NodeFormat_ImportSpec> { /* compiled code */ }

    func GetRootAsAnnotation(): std.core.Option<std.ast.NodeFormat_Annotation> { /* compiled code */ }
}

class NodeFormat_MacroInvocation <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let FULLNAME: UInt16

    let IDENTIFIER: UInt16

    let IDENTIFIERPOS: UInt16

    let LEFTSQUAREPOS: UInt16

    let RIGHTSQUAREPOS: UInt16

    let LEFTPARENPOS: UInt16

    let RIGHTPARENPOS: UInt16

    let ATPOS: UInt16

    let ATTRS: UInt16

    let ARGS: UInt16

    let DECL: UInt16

    let HASPARENTHESIS: UInt16

    let IS_COMPILE_TIME_VISIBLE: UInt16

    func GetFullName(): std.core.String { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightSquarePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetRightParenPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAtPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAttrs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }

    func GetArgs(): std.core.Array<std.core.Option<std.ast.NodeFormat_Token>> { /* compiled code */ }

    func GetDecl(): std.core.Option<std.ast.NodeFormat_Decl> { /* compiled code */ }

    func GetHasParenthesis(): Bool { /* compiled code */ }

    func GetIsCompileTimeVisible(): Bool { /* compiled code */ }
}

class NodeFormat_File <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FILENAME: UInt16

    let FILEPATH: UInt16

    let PACKAGE: UInt16

    let IMPORTS: UInt16

    let DECLS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetFileName(): std.core.String { /* compiled code */ }

    func GetFilePath(): std.core.String { /* compiled code */ }

    func GetPackage(): std.core.Option<std.ast.NodeFormat_PackageSpec> { /* compiled code */ }

    func GetImports(): std.core.Array<std.core.Option<std.ast.NodeFormat_ImportSpec>> { /* compiled code */ }

    func GetDecls(): std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>> { /* compiled code */ }
}

class NodeFormat_PackageSpec <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let MACROPOS: UInt16

    let PACKAGEPOS: UInt16

    let PREFIXPATHS: UInt16

    let PREFIXPOSES: UInt16

    let PREFIXDOTPOSES: UInt16

    let PACKAGENAME: UInt16

    let PACKAGENAMEPOS: UInt16

    let ACCESSIBLE: UInt16

    let HASMACRO: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetMacroPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPackagePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetPrefixPaths(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    func GetPrefixPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetPrefixDotPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetPackageName(): std.core.String { /* compiled code */ }

    func GetPackageNamePos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAccessible(): std.ast.NodeFormat_AccessibleKind { /* compiled code */ }

    func GetHasMacro(): Bool { /* compiled code */ }
}

class NodeFormat_ImportSpec <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REEXPORT: UInt16

    let IMPORTPOS: UInt16

    let CONTENT: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetReExport(): std.ast.NodeFormat_ReExportKind { /* compiled code */ }

    func GetImportPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetContent(): std.core.Option<std.ast.NodeFormat_ImportContent> { /* compiled code */ }
}

class NodeFormat_ImportContent <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    let PREFIXPATHS: UInt16

    let PREFIXPOSES: UInt16

    let PREFIXDOTPOSES: UInt16

    let IDENTIFIER: UInt16

    let IDENTIFIERPOS: UInt16

    let ASPOS: UInt16

    let ASIDENTIFIER: UInt16

    let ASIDENTIFIERPOS: UInt16

    let LEFTCURLPOS: UInt16

    let ITEMS: UInt16

    let COMMAPOSES: UInt16

    let RIGHTCURLPOS: UInt16

    func GetBase(): std.core.Option<std.ast.NodeFormat_NodeBase> { /* compiled code */ }

    func GetKind(): std.ast.NodeFormat_ImportKind { /* compiled code */ }

    func GetPrefixPaths(): std.core.Array<std.core.Option<std.core.String>> { /* compiled code */ }

    func GetPrefixPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetPrefixDotPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetIdentifier(): std.core.String { /* compiled code */ }

    func GetIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAsPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetAsIdentifier(): std.core.String { /* compiled code */ }

    func GetAsIdentifierPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetLeftCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }

    func GetItems(): std.core.Array<std.core.Option<std.ast.NodeFormat_ImportContent>> { /* compiled code */ }

    func GetCommaPoses(): std.core.Array<std.ast.NodeFormat_Position> { /* compiled code */ }

    func GetRightCurlPos(): std.ast.NodeFormat_Position { /* compiled code */ }
}

func isValidPosition(pos: std.ast.NodeFormat_Position): Bool { /* compiled code */ }

func isValidPosition(tk: std.ast.Token): Bool { /* compiled code */ }

struct Token <: std.ast.ToBytes {
    let kind: std.ast.TokenKind

    let value: std.core.String

    let pos: std.ast.Position

    var delimiterNum: UInt16

    var isSingleQuote: Bool

    init() { /* compiled code */ }

    init(kind: std.ast.TokenKind) { /* compiled code */ }

    init(kind: std.ast.TokenKind, value: std.core.String) { /* compiled code */ }

    init(k: std.ast.TokenKind, v: std.core.String, f: UInt32, l: Int32, c: Int32) { /* compiled code */ }

    func addPosition(fileID: UInt32, line: Int32, colum: Int32): std.ast.Token { /* compiled code */ }

    func addPosition(position: std.ast.NodeFormat_Position): std.ast.Token { /* compiled code */ }

    func addPosition(position: std.ast.Position): std.ast.Token { /* compiled code */ }

    operator func ==(r: std.ast.Token): Bool { /* compiled code */ }

    operator func !=(r: std.ast.Token): Bool { /* compiled code */ }

    operator func +(r: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    operator func +(r: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    func dump(): Unit { /* compiled code */ }

    func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64 { /* compiled code */ }

    func getSize(): Int64 { /* compiled code */ }
}

func isValidToken(tk: std.ast.Token): Bool { /* compiled code */ }

func getNodeBeginPos(nodeBase: std.core.Option<std.ast.NodeFormat_NodeBase>): std.ast.NodeFormat_Position { /* compiled code */ }

func getOperaterKindOrIdentTokens(opKind: Int32, pos: std.ast.NodeFormat_Position, ident: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

class Tokens <: std.core.Object & std.core.ToString & std.core.Iterable<std.ast.Token> & std.ast.ToBytes {
    var tokens: std.collection.ArrayList<std.ast.Token>

    func refreshPosition(): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }

    init(buf: std.core.Array<UInt8>) { /* compiled code */ }

    init(tokArray: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(tokArrayList: std.collection.ArrayList<std.ast.Token>) { /* compiled code */ }

    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func `get`(index: Int64): std.ast.Token { /* compiled code */ }

    func iterator(): std.ast.TokensIterator { /* compiled code */ }

    func concat(tokens: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    operator func [](index: Int64): std.ast.Token { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>): std.ast.Tokens { /* compiled code */ }

    operator func +(r: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    operator func +(r: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    func append(tokens: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    func append(token: std.ast.Token): std.ast.Tokens { /* compiled code */ }

    func append(node: std.ast.Node): std.ast.Tokens { /* compiled code */ }

    func remove(index: Int64): std.ast.Tokens { /* compiled code */ }

    func dump(): Unit { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func toBytes(startFrom: Int64): std.core.Array<UInt8> { /* compiled code */ }

    func getSize(startFrom: Int64): Int64 { /* compiled code */ }
}

func getIndent(indent: UInt16): std.core.String { /* compiled code */ }

func getTokenIndent(name: std.core.String, tk: std.ast.Token, indent: UInt16, noNameIndent: Bool, index: std.core.Option<Int64>): std.core.String { /* compiled code */ }

func dumpNodes(name: std.core.String, contents: std.collection.ArrayList<N>, indent: UInt16): std.core.String { /* compiled code */ }

class Node <: std.core.Object & std.ast.ToTokens {
    init() { /* compiled code */ }

    var begin_: std.ast.Position

    var end_: std.ast.Position

    var astKind: std.core.String

    var currentIndent: UInt16

    var beginNode: Bool

    mut prop beginPos: std.ast.Position {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop endPos: std.ast.Position {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func compareTokens(tokens1: std.ast.Tokens, tokens2: std.ast.Tokens): Bool { /* compiled code */ }

func checkKindValue(k: std.ast.TokenKind, v: std.core.String): Bool { /* compiled code */ }

enum TokenKind <: std.core.ToString {
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

    operator func ==(right: std.ast.TokenKind): Bool { /* compiled code */ }

    operator func !=(right: std.ast.TokenKind): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func opPrecedence(): Int64 { /* compiled code */ }

    func isNonAssociative(): Bool { /* compiled code */ }
}

func getImportSpecTokens(imports: std.collection.ArrayList<std.ast.ImportList>): std.ast.Tokens { /* compiled code */ }

class ImportList <: std.ast.Node {
    init(reExportToken: std.ast.Token, imp: std.ast.Token, content: std.ast.ImportContent) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var reExportToken_: std.ast.Token

    var import_: std.ast.Token

    var content_: std.ast.ImportContent

    let innerToken_: std.ast.Tokens

    mut prop modifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop content: std.ast.ImportContent {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isImportMulti(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func dumpCommonContent(currentIndent: UInt16, upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, body: std.ast.Body): std.core.String { /* compiled code */ }

class TypeNode <: std.ast.Node {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var typeParameterName_: std.ast.Token

    var colon_: std.ast.Token

    mut prop typeParameterName: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class GenericConstraint <: std.ast.Node {
    init(keyword: std.ast.Token, ty: std.ast.TypeNode, upbound: std.ast.Token, upperBounds: std.collection.ArrayList<std.ast.TypeNode>, bitAnds: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var ty_: std.ast.TypeNode

    var upbound_: std.ast.Token

    var upperBounds_: std.collection.ArrayList<std.ast.TypeNode>

    var bitAnds_: std.ast.Tokens

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArgument: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop upperBounds: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop bitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Body <: std.ast.Node {
    init(lBrace: std.ast.Token, decls: std.collection.ArrayList<std.ast.Decl>, rBrace: std.ast.Token) { /* compiled code */ }

    init(decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init() { /* compiled code */ }

    var lBrace_: std.ast.Token

    var decls_: std.collection.ArrayList<std.ast.Decl>

    var rBrace_: std.ast.Token

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func referenceType2Tokens(upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, constraintCommas: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

extend Tokens {
    func tryGet(index: Int64): std.core.Option<std.ast.Token> { /* compiled code */ }
}

extend Token {
    func tryGet(): std.core.Option<std.ast.Token> { /* compiled code */ }
}

func checkTokenType(tk: std.ast.Token, kind: std.ast.TokenKind): Unit { /* compiled code */ }

func checkTokensType(tks: std.ast.Tokens, kind: std.ast.TokenKind): Unit { /* compiled code */ }

func checkTokenTypeAssign(tk: std.ast.Token): Unit { /* compiled code */ }

func checkPrimitiveType(tk: std.ast.Token): Unit { /* compiled code */ }

func checkValid(input: std.ast.Token): std.ast.Token { /* compiled code */ }

class Decl <: std.ast.Node {
    init(annotations: std.collection.ArrayList<std.ast.Annotation>, modifiers: std.collection.ArrayList<std.ast.Modifier>, keyword: std.ast.Token, identifier: std.ast.Token, genericParam: std.core.Option<std.ast.GenericParam>, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, constraintCommas: std.ast.Tokens, isGenericDecl: Bool) { /* compiled code */ }

    init(keyword: std.ast.Token, identifier: std.ast.Token) { /* compiled code */ }

    init(decl: std.ast.Decl) { /* compiled code */ }

    init(input: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var annotations_: std.collection.ArrayList<std.ast.Annotation>

    var modifiers_: std.collection.ArrayList<std.ast.Modifier>

    var keyword_: std.ast.Token

    var identifier_: std.ast.Token

    var genericParam_: std.core.Option<std.ast.GenericParam>

    var constraint_: std.collection.ArrayList<std.ast.GenericConstraint>

    var constraintCommas_: std.ast.Tokens

    var isGenericDecl_: Bool

    var node: std.ast.Node

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop annotations: std.collection.ArrayList<std.ast.Annotation> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop modifiers: std.collection.ArrayList<std.ast.Modifier> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop genericParam: std.ast.GenericParam {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop genericConstraint: std.collection.ArrayList<std.ast.GenericConstraint> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop constraintCommas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func hasAttr(attr: std.core.String): Bool { /* compiled code */ }

    func getAttrs(): std.ast.Tokens { /* compiled code */ }

    mut prop isGenericDecl: Bool {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Annotation <: std.ast.Node {
    init(at: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rSquare: std.ast.Token, attrs: std.ast.Tokens, cond: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var innerToken_: std.ast.Tokens

    var at_: std.ast.Token

    var identifier_: std.ast.Token

    var lSquare_: std.ast.Token

    var args_: std.collection.ArrayList<std.ast.Argument>

    var rSquare_: std.ast.Token

    var attrs_: std.ast.Tokens

    var cond_: std.core.Option<std.ast.Expr>

    mut prop at: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop attributes: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Modifier <: std.ast.Node {
    init(keyword: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var kind_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class GenericParam <: std.ast.Node {
    init(lAngle: std.ast.Token, parameters: std.ast.Tokens, rAngle: std.ast.Token, commas: std.ast.Tokens) { /* compiled code */ }

    init(parameters: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lAngle_: std.ast.Token

    var parameters_: std.ast.Tokens

    var rAngle_: std.ast.Token

    var commas_: std.ast.Tokens

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop parameters: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Visitor {
    var breakTraverseFlag: Bool

    func needBreakTraverse(): Bool { /* compiled code */ }

    func breakTraverse(): Unit { /* compiled code */ }

    func visit(_: std.ast.Node): Unit { /* compiled code */ }

    func visit(_: std.ast.Expr): Unit { /* compiled code */ }

    func visit(_: std.ast.IsExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.AsExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.LitConstExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.UnaryExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.BinaryExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.ParenExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.CallExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.RefExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.ReturnExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.AssignExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.MemberAccess): Unit { /* compiled code */ }

    func visit(_: std.ast.IfExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.LetPatternExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.MatchExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.WhileExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.LambdaExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.SpawnExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.SynchronizedExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.TrailingClosureExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.TypeConvExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.ForInExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.PrimitiveTypeExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.ArrayLiteral): Unit { /* compiled code */ }

    func visit(_: std.ast.TupleLiteral): Unit { /* compiled code */ }

    func visit(_: std.ast.SubscriptExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.RangeExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.Block): Unit { /* compiled code */ }

    func visit(_: std.ast.DoWhileExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.JumpExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.IncOrDecExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.TryExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.ThrowExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.OptionalExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.QuoteExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.MacroExpandExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.WildcardExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.VArrayExpr): Unit { /* compiled code */ }

    func visit(_: std.ast.Decl): Unit { /* compiled code */ }

    func visit(_: std.ast.ClassDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.FuncDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.MainDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.MacroDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.StructDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.InterfaceDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.PropDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.VarDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.PrimaryCtorDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.EnumDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.FuncParam): Unit { /* compiled code */ }

    func visit(_: std.ast.TypeAliasDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.ExtendDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.MacroExpandDecl): Unit { /* compiled code */ }

    func visit(_: std.ast.TypeNode): Unit { /* compiled code */ }

    func visit(_: std.ast.RefType): Unit { /* compiled code */ }

    func visit(_: std.ast.PrimitiveType): Unit { /* compiled code */ }

    func visit(_: std.ast.FuncType): Unit { /* compiled code */ }

    func visit(_: std.ast.ThisType): Unit { /* compiled code */ }

    func visit(_: std.ast.ParenType): Unit { /* compiled code */ }

    func visit(_: std.ast.QualifiedType): Unit { /* compiled code */ }

    func visit(_: std.ast.PrefixType): Unit { /* compiled code */ }

    func visit(_: std.ast.TupleType): Unit { /* compiled code */ }

    func visit(_: std.ast.VArrayType): Unit { /* compiled code */ }

    func visit(_: std.ast.Pattern): Unit { /* compiled code */ }

    func visit(_: std.ast.ConstPattern): Unit { /* compiled code */ }

    func visit(_: std.ast.WildcardPattern): Unit { /* compiled code */ }

    func visit(_: std.ast.VarPattern): Unit { /* compiled code */ }

    func visit(_: std.ast.VarOrEnumPattern): Unit { /* compiled code */ }

    func visit(_: std.ast.ExceptTypePattern): Unit { /* compiled code */ }

    func visit(_: std.ast.TypePattern): Unit { /* compiled code */ }

    func visit(_: std.ast.EnumPattern): Unit { /* compiled code */ }

    func visit(_: std.ast.TuplePattern): Unit { /* compiled code */ }

    func visit(_: std.ast.GenericParam): Unit { /* compiled code */ }

    func visit(_: std.ast.GenericConstraint): Unit { /* compiled code */ }

    func visit(_: std.ast.MatchCase): Unit { /* compiled code */ }

    func visit(_: std.ast.Program): Unit { /* compiled code */ }

    func visit(_: std.ast.PackageHeader): Unit { /* compiled code */ }

    func visit(_: std.ast.ImportList): Unit { /* compiled code */ }

    func visit(_: std.ast.ImportContent): Unit { /* compiled code */ }

    func visit(_: std.ast.Constructor): Unit { /* compiled code */ }

    func visit(_: std.ast.Argument): Unit { /* compiled code */ }

    func visit(_: std.ast.Annotation): Unit { /* compiled code */ }

    func visit(_: std.ast.Modifier): Unit { /* compiled code */ }

    func visit(_: std.ast.Body): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class ClassDecl <: std.ast.Decl {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var classbody_: std.ast.Body

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class StructDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var structbody_: std.ast.Body

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class InterfaceDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var interfacetbody_: std.ast.Body

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class EnumDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, lBrace: std.ast.Token, bitor: std.ast.Tokens, constructors: std.collection.ArrayList<std.ast.Constructor>, decls: std.collection.ArrayList<std.ast.Decl>, rBrace: std.ast.Token, ellipsis: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var lBrace_: std.ast.Token

    var bitor_: std.ast.Tokens

    var constructors_: std.collection.ArrayList<std.ast.Constructor>

    var decls_: std.collection.ArrayList<std.ast.Decl>

    var rBrace_: std.ast.Token

    var ellipsis_: std.ast.Token

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop constructors: std.collection.ArrayList<std.ast.Constructor> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop ellipsis: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Constructor <: std.ast.Node {
    init() { /* compiled code */ }

    init(annotations: std.collection.ArrayList<std.ast.Annotation>, identifier: std.ast.Token) { /* compiled code */ }

    init(annotations: std.collection.ArrayList<std.ast.Annotation>, identifier: std.ast.Token, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    var identifier_: std.ast.Token

    var lParen_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    var annotations_: std.collection.ArrayList<std.ast.Annotation>

    mut prop annotations: std.collection.ArrayList<std.ast.Annotation> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ExtendDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, extendType: std.ast.TypeNode, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var extendType_: std.ast.TypeNode

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var body_: std.ast.Body

    override mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop extendType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop upperBound: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypes: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop superTypeBitAnds: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class FuncDecl <: std.ast.Decl {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(decl: std.ast.Decl, lParen: std.ast.Token, funcParams: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init() { /* compiled code */ }

    var overloadedOperators_: std.ast.Tokens

    var lParen_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.FuncParam>

    var rParen_: std.ast.Token

    var colon_: std.ast.Token

    var retTy_: std.core.Option<std.ast.TypeNode>

    var block_: std.ast.Block

    var isEnumConstruct: Bool

    mut prop overloadOp: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isConst(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class FuncParam <: std.ast.Decl {
    init(decl: std.ast.Decl, notMark: std.ast.Token, colon: std.ast.Token, paramType: std.ast.TypeNode, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>, comma: std.ast.Token, isMemberParam: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(key: std.ast.Token, identifier: std.ast.Token) { /* compiled code */ }

    var not_: std.ast.Token

    var colon_: std.ast.Token

    var paramType_: std.ast.TypeNode

    var assign_: std.ast.Token

    var expr_: std.core.Option<std.ast.Expr>

    var comma_: std.ast.Token

    var isMemberParam_: Bool

    mut prop not: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop paramType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isMemberParam(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Block <: std.ast.Expr {
    init(lCurl: std.ast.Token, nodes: std.collection.ArrayList<std.ast.Node>, rCurl: std.ast.Token, unsafe_: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var lCurl_: std.ast.Token

    var nodes_: std.collection.ArrayList<std.ast.Node>

    var rCurl_: std.ast.Token

    var unsafe_: std.ast.Token

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop nodes: std.collection.ArrayList<std.ast.Node> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MainDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, funcParams: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var rParen_: std.ast.Token

    var colon_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.FuncParam>

    var retTy_: std.core.Option<std.ast.TypeNode>

    var block_: std.ast.Block

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MacroDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, colon: std.ast.Token, ty: std.core.Option<std.ast.TypeNode>, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var rParen_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.FuncParam>

    var colon_: std.ast.Token

    var retTy_: std.core.Option<std.ast.TypeNode>

    var block_: std.ast.Block

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class PrimaryCtorDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var rParen_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.FuncParam>

    var block_: std.ast.Block

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isConst(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class VarDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, colon: std.ast.Token, declType: std.core.Option<std.ast.TypeNode>, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(decl: std.ast.Decl, pattern: std.core.Option<std.ast.Pattern>, colon: std.ast.Token, declType: std.core.Option<std.ast.TypeNode>, assign: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var pattern_: std.core.Option<std.ast.Pattern>

    var colon_: std.ast.Token

    var declType_: std.core.Option<std.ast.TypeNode>

    var assign_: std.ast.Token

    var expr_: std.core.Option<std.ast.Expr>

    var isEnumConstruct: Bool

    mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isConst(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Expr <: std.ast.Node {
    init() { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(_: UInt16): std.core.String { /* compiled code */ }

    func precedence(): Int64 { /* compiled code */ }
}

class Pattern <: std.ast.Node {
    init() { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(_: UInt16): std.core.String { /* compiled code */ }
}

class PropDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, colon: std.ast.Token, declType: std.ast.TypeNode, lCurl: std.ast.Token, getter: std.core.Option<std.ast.FuncDecl>, setter: std.core.Option<std.ast.FuncDecl>, rCurl: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var colon_: std.ast.Token

    var declType_: std.ast.TypeNode

    var lCurl_: std.ast.Token

    var getter_: std.core.Option<std.ast.FuncDecl>

    var setter_: std.core.Option<std.ast.FuncDecl>

    var rCurl_: std.ast.Token

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop declType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop getter: std.ast.FuncDecl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop setter: std.ast.FuncDecl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TypeAliasDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, assign: std.ast.Token, aliasType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var assign_: std.ast.Token

    var aliasType_: std.ast.TypeNode

    mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop aliasType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MacroExpandDecl <: std.ast.Decl {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool, isCompileTimeVisible: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var pkgIdentifier_: std.ast.Token

    var lSquare_: std.ast.Token

    var macroAttrs_: std.ast.Tokens

    var rSquare_: std.ast.Token

    var lParen_: std.ast.Token

    var macroInputs_: std.ast.Tokens

    var rParen_: std.ast.Token

    var macroInputDecl_: std.core.Option<std.ast.Decl>

    var hasParenthesis_: Bool

    var isCompileTimeVisible_: Bool

    mut prop fullIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroInputDecl: std.ast.Decl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MacroExpandParam <: std.ast.FuncParam {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool, isCompileTimeVisible: Bool) { /* compiled code */ }

    init() { /* compiled code */ }

    var pkgIdentifier_: std.ast.Token

    var lSquare_: std.ast.Token

    var macroAttrs_: std.ast.Tokens

    var rSquare_: std.ast.Token

    var lParen_: std.ast.Token

    var macroInputs_: std.ast.Tokens

    var rParen_: std.ast.Token

    var macroInputDecl_: std.core.Option<std.ast.Decl>

    var hasParenthesis_: Bool

    var isCompileTimeVisible_: Bool

    mut prop fullIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroInputDecl: std.ast.Decl {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func boundsCheck(buf: std.core.Array<UInt8>, sz: Int64): Bool { /* compiled code */ }

func getUInt8(buf: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

func getBool(buf: std.core.Array<UInt8>): Bool { /* compiled code */ }

func getUInt16(buf: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

func getUInt32(buf: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

func getInt32(buf: std.core.Array<UInt8>): Int32 { /* compiled code */ }

func getUInt64(buf: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

func getString(buf: std.core.Array<UInt8>, offset: Int64, len: UInt32): std.core.String { /* compiled code */ }

enum DiagReportLevel {
    | ERROR

    | WARNING

    func level(): Int32 { /* compiled code */ }
}

func diagReport(level: std.ast.DiagReportLevel, tokens: std.ast.Tokens, message: std.core.String, hint: std.core.String): Unit { /* compiled code */ }

class ASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class ParseASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class BinaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(leftExpr: std.ast.Expr, op: std.ast.Token, rightExpr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    var lExpr_: std.ast.Expr

    var rExpr_: std.ast.Expr

    var op_: std.ast.Token

    mut prop leftExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rightExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class UnaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(op: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class IsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, shiftType: std.ast.TypeNode) { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    var shiftType_: std.ast.TypeNode

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop shiftType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class AsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    var shiftType_: std.ast.TypeNode

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop shiftType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ParenExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    var lParen_: std.ast.Token

    var expr_: std.ast.Expr

    var rParen_: std.ast.Token

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop parenthesizedExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class LitConstExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(literal: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var literal_: std.ast.Token

    mut prop literal: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class RefExpr <: std.ast.Expr {
    init(identifier: std.ast.Token, lAngle: std.ast.Token, typeArgs: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    var lAngle_: std.ast.Token

    var typeArgs_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rAngle_: std.ast.Token

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ReturnExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    var expr_: std.core.Option<std.ast.Expr>

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ThrowExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    var expr_: std.ast.Expr

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class AssignExpr <: std.ast.Expr {
    init(lexpr: std.ast.Expr, assign: std.ast.Token, rexpr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var assign_: std.ast.Token

    var lexpr_: std.ast.Expr

    var rexpr_: std.ast.Expr

    mut prop assign: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop leftExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rightExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class CallExpr <: std.ast.Expr {
    init(callSuffix: std.ast.Expr, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var postfixExpr_: std.ast.Expr

    var lParen_: std.ast.Token

    var args_: std.collection.ArrayList<std.ast.Argument>

    var rParen_: std.ast.Token

    mut prop callFunc: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class Argument <: std.ast.Node {
    init(inout_: std.ast.Token, identifier: std.ast.Token, colon: std.ast.Token, expr: std.ast.Expr, comma: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var inout_: std.ast.Token

    var identifier_: std.ast.Token

    var colon_: std.ast.Token

    var expr_: std.ast.Expr

    var comma_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MemberAccess <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(postfixExpr: std.ast.Expr, dot: std.ast.Token, ident: std.ast.Token, lAngle: std.ast.Token, typeArgs: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    var postfixExpr_: std.ast.Expr

    var dot_: std.ast.Token

    var ident_: std.ast.Token

    var lAngle_: std.ast.Token

    var typeArgs_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rAngle_: std.ast.Token

    mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop dot: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop field: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class IfExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWords: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token, ifBlock: std.ast.Block, elseTk: std.ast.Token, elseExpr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    var if_: std.ast.Token

    var lParen_: std.ast.Token

    var cond_: std.ast.Expr

    var rParen_: std.ast.Token

    var else_: std.ast.Token

    var ifBlock_: std.ast.Block

    var elseExpr_: std.core.Option<std.ast.Expr>

    mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop ifBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordE: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop elseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class LetPatternExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(key: std.ast.Token, pattern: std.ast.Pattern, backArrow: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    var key_: std.ast.Token

    var pattern_: std.ast.Pattern

    var backArrow_: std.ast.Token

    var expr_: std.ast.Expr

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop backArrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MatchExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, selector: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, lCurl: std.ast.Token, matchCases: std.collection.ArrayList<std.ast.MatchCase>, rCurl: std.ast.Token) { /* compiled code */ }

    var keyWord_: std.ast.Token

    var lParen_: std.ast.Token

    var selector_: std.core.Option<std.ast.Expr>

    var rParen_: std.ast.Token

    var lCurl_: std.ast.Token

    var matchCases_: std.collection.ArrayList<std.ast.MatchCase>

    var rCurl_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop selector: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop matchCases: std.collection.ArrayList<std.ast.MatchCase> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MatchCase <: std.ast.Node {
    init(case_: std.ast.Token, expr: std.core.Option<std.ast.Expr>, arrow: std.ast.Token, exprOrDecl: std.ast.Block) { /* compiled code */ }

    init(case_: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, bitOrs: std.ast.Tokens, where_: std.ast.Token, patternGuard: std.core.Option<std.ast.Expr>, arrow: std.ast.Token, exprOrDecl: std.ast.Block) { /* compiled code */ }

    init() { /* compiled code */ }

    var case_: std.ast.Token

    var expr_: std.core.Option<std.ast.Expr>

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var bitOrs_: std.ast.Tokens

    var where_: std.ast.Token

    var patternGuard_: std.core.Option<std.ast.Expr>

    var arrow_: std.ast.Token

    var exprOrDecl_: std.ast.Block

    mut prop keywordC: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop bitOrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patternGuard: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop arrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class WhileExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    var keyWord_: std.ast.Token

    var lParen_: std.ast.Token

    var cond_: std.ast.Expr

    var rParen_: std.ast.Token

    var block_: std.ast.Block

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class DoWhileExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(do_: std.ast.Token, block: std.ast.Block, while_: std.ast.Token, lParen: std.ast.Token, cond: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    var do_: std.ast.Token

    var block_: std.ast.Block

    var while_: std.ast.Token

    var lParen_: std.ast.Token

    var cond_: std.ast.Expr

    var rParen_: std.ast.Token

    mut prop keywordD: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop condition: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class LambdaExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(lCurl: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, db_arrow: std.ast.Token, nodes: std.collection.ArrayList<std.ast.Node>, rCurl: std.ast.Token, mockSupported: Bool) { /* compiled code */ }

    var lCurl_: std.ast.Token

    var funcParams_: std.collection.ArrayList<std.ast.FuncParam>

    var db_arrow_: std.ast.Token

    var nodes_: std.collection.ArrayList<std.ast.Node>

    var rCurl_: std.ast.Token

    var mockSupported_: Bool

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop funcParams: std.collection.ArrayList<std.ast.FuncParam> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop doubleArrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop nodes: std.collection.ArrayList<std.ast.Node> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class SpawnExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, context: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, task: std.ast.LambdaExpr) { /* compiled code */ }

    var keyWord_: std.ast.Token

    var lParen_: std.ast.Token

    var context_: std.core.Option<std.ast.Expr>

    var rParen_: std.ast.Token

    var task_: std.ast.LambdaExpr

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop threadContext: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lambdaExpr: std.ast.LambdaExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class SynchronizedExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(keyWord: std.ast.Token, lParen: std.ast.Token, mutex: std.ast.Expr, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    var keyWord_: std.ast.Token

    var lParen_: std.ast.Token

    var mutex_: std.ast.Expr

    var rParen_: std.ast.Token

    var block_: std.ast.Block

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop structuredMutex: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TrailingClosureExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, lambda: std.ast.LambdaExpr) { /* compiled code */ }

    var expr_: std.ast.Expr

    var lambda_: std.ast.LambdaExpr

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lambdaExpr: std.ast.LambdaExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TypeConvExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(ty: std.ast.PrimitiveType, lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    var type_: std.ast.PrimitiveType

    var lParen_: std.ast.Token

    var expr_: std.ast.Expr

    var rParen_: std.ast.Token

    mut prop targetType: std.ast.PrimitiveType {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class PrimitiveType <: std.ast.TypeNode {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    var keyword_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ForInExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(for_: std.ast.Token, lParen: std.ast.Token, pattern: std.ast.Pattern, in_: std.ast.Token, expr: std.ast.Expr, where_: std.ast.Token, patternGuard: std.core.Option<std.ast.Expr>, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    var for_: std.ast.Token

    var lParen_: std.ast.Token

    var pattern_: std.ast.Pattern

    var in_: std.ast.Token

    var expr_: std.ast.Expr

    var where_: std.ast.Token

    var patternGuard_: std.core.Option<std.ast.Expr>

    var rParen_: std.ast.Token

    var block_: std.ast.Block

    mut prop keywordF: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordI: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordW: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patternGuard: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop block: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class PrimitiveTypeExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(kind: std.ast.Token) { /* compiled code */ }

    var keyword_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ArrayLiteral <: std.ast.Expr {
    init(lSquare: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lSquare_: std.ast.Token

    var elements_: std.collection.ArrayList<std.ast.Expr>

    var commas_: std.ast.Tokens

    var rSquare_: std.ast.Token

    mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop elements: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TupleLiteral <: std.ast.Expr {
    init(lParen: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var elements_: std.collection.ArrayList<std.ast.Expr>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop elements: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class RangeExpr <: std.ast.Expr {
    init(start: std.core.Option<std.ast.Expr>, rangeOp: std.ast.Token, end: std.core.Option<std.ast.Expr>, colon: std.ast.Token, step: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var start_: std.core.Option<std.ast.Expr>

    var rangeOp_: std.ast.Token

    var endExpr_: std.core.Option<std.ast.Expr>

    var colon_: std.ast.Token

    var step_: std.core.Option<std.ast.Expr>

    mut prop start: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop end: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop step: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class SubscriptExpr <: std.ast.Expr {
    init(postfixExpr: std.ast.Expr, lSquare: std.ast.Token, index: std.collection.ArrayList<std.ast.Expr>, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var postfixExpr_: std.ast.Expr

    var lSquare_: std.ast.Token

    var index_: std.collection.ArrayList<std.ast.Expr>

    var rSquare_: std.ast.Token

    mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop indexList: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class JumpExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var kind_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class IncOrDecExpr <: std.ast.Expr {
    init(kind: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var op_: std.ast.Token

    var expr_: std.ast.Expr

    mut prop op: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop expr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TryExpr <: std.ast.Expr {
    init(try_: std.ast.Token, resourceSpecLParen: std.ast.Token, resourceSpec: std.collection.ArrayList<std.ast.VarDecl>, resourceSpecCommas: std.ast.Tokens, resourceSpecRParen: std.ast.Token, tryBlock: std.ast.Block, catches: std.ast.Tokens, catchLParens: std.ast.Tokens, catchRParens: std.ast.Tokens, patterns: std.collection.ArrayList<std.ast.Pattern>, catchBlocks: std.collection.ArrayList<std.ast.Block>, finally_: std.ast.Token, finallyBlock: std.core.Option<std.ast.Block>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var try_: std.ast.Token

    var resourceSpecLParen_: std.ast.Token

    var resourceSpec_: std.collection.ArrayList<std.ast.VarDecl>

    var resourceSpecCommas_: std.ast.Tokens

    var resourceSpecRParen_: std.ast.Token

    var tryBlock_: std.ast.Block

    var catches_: std.ast.Tokens

    var catchLParens_: std.ast.Tokens

    var catchRParens_: std.ast.Tokens

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var catchBlocks_: std.collection.ArrayList<std.ast.Block>

    var finally_: std.ast.Token

    var finallyBlock_: std.core.Option<std.ast.Block>

    mut prop keywordT: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop resourceSpec: std.collection.ArrayList<std.ast.VarDecl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop tryBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordsC: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop catchPatterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop catchBlocks: std.collection.ArrayList<std.ast.Block> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordF: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop finallyBlock: std.ast.Block {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class OptionalExpr <: std.ast.Expr {
    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(baseExpr: std.ast.Expr, quest: std.ast.Token) { /* compiled code */ }

    var baseExpr_: std.ast.Expr

    var quest_: std.ast.Token

    mut prop baseExpr: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop quest: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func precedence(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class QuoteToken <: std.ast.Expr {
    init(tokens: std.ast.Tokens) { /* compiled code */ }

    var tokens_: std.ast.Tokens

    mut prop tokens: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class QuoteExpr <: std.ast.Expr {
    init(keyword: std.ast.Token, lParen: std.ast.Token, exprs: std.collection.ArrayList<std.ast.Expr>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var lParen_: std.ast.Token

    var exprs_: std.collection.ArrayList<std.ast.Expr>

    var rParen_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop exprs: std.collection.ArrayList<std.ast.Expr> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class WildcardExpr <: std.ast.Expr {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class VArrayExpr <: std.ast.Expr {
    init(ty: std.ast.VArrayType, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var ty_: std.ast.VArrayType

    var lParen_: std.ast.Token

    var args_: std.collection.ArrayList<std.ast.Argument>

    var rParen_: std.ast.Token

    mut prop vArrayType: std.ast.VArrayType {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop arguments: std.collection.ArrayList<std.ast.Argument> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class VArrayType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token, lAngle: std.ast.Token, elementTy: std.ast.TypeNode, dollar: std.ast.Token, size: std.ast.Token, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var lAngle_: std.ast.Token

    var elementTy_: std.ast.TypeNode

    var dollar_: std.ast.Token

    var constant_: std.ast.Token

    var rAngle_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop elementTy: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop dollar: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop size: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class MacroExpandExpr <: std.ast.Expr {
    init(at: std.ast.Token, fullIdentifier: std.ast.Token, identifier: std.ast.Token, lSquare: std.ast.Token, macroAttrs: std.ast.Tokens, rSquare: std.ast.Token, lParen: std.ast.Token, macroInputs: std.ast.Tokens, rParen: std.ast.Token, macroInputDecl: std.core.Option<std.ast.Decl>, hasParenthesis: Bool) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var at_: std.ast.Token

    var pkgIdentifier_: std.ast.Token

    var identifier_: std.ast.Token

    var lSquare_: std.ast.Token

    var macroAttrs_: std.ast.Tokens

    var rSquare_: std.ast.Token

    var lParen_: std.ast.Token

    var macroInputs_: std.ast.Tokens

    var rParen_: std.ast.Token

    var macroInputDecl_: std.core.Option<std.ast.Decl>

    var hasParenthesis_: Bool

    mut prop at: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroAttrs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rSquare: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop macroInputs: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func addParen(ret: std.ast.Tokens, innerExpr: std.ast.Expr, thisExpr: std.ast.Expr, addOnEqualPred: Bool): std.ast.Tokens { /* compiled code */ }

let MACRO_OBJECT: std.core.ThreadLocal<CPointer<Unit>>

foreign func CJ_AST_ParseExpr(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

struct ParseRes {
    let node: CPointer<UInt8>

    let errMsg: CString

    init() { /* compiled code */ }
}

foreign func CJ_AST_ParseDecl(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParsePropMemberDecl(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParsePrimaryConstructor(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParsePattern(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParseType(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParseTopLevel(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_ParseAnnotationArguments(tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

foreign func CJ_AST_Lex(fptr: CPointer<Unit>, code: CString): CPointer<UInt8>

foreign func CJ_CheckParentContext(fptr: CPointer<Unit>, parent: CString, report: Bool): Bool

foreign func CJ_SetItemInfo(fptr: CPointer<Unit>, key: CString, value: CPointer<Unit>, valueType: UInt8): Unit

foreign func CJ_GetChildMessages(fptr: CPointer<Unit>, children: CString): CPointer<CPointer<CPointer<Unit>>>

foreign func CJ_CheckAddSpace(tokBytes: CPointer<UInt8>, spaceFlag: CPointer<Bool>): Unit

foreign func CJ_AST_DiagReport(fptr: CPointer<Unit>, level: CPointer<Int32>, tokensBytes: CPointer<UInt8>, message: CString, hint: CString): UInt8

foreign func CJ_GetMacroPosition(fptr: CPointer<Unit>, fileID: CPointer<UInt32>, line: CPointer<Int32>, column: CPointer<Int32>): Unit

foreign func CJ_AST_Float64ToCPointer(num: Float64): CString

let BUFFER_HEADER_SIZE: Int64

func getInt32FromUnsafePointer(ptr: CPointer<UInt8>): Int32 { /* compiled code */ }

func parse(input: std.ast.Tokens, nativeParse: (CPointer<UInt8>) -> CPointer<std.ast.ParseRes>): std.ast.NodeFormat_Node { /* compiled code */ }

func parseFragment(input: std.ast.Tokens, startFrom: Int64, nativeParse: (CPointer<UInt8>, CPointer<Int64>) -> CPointer<std.ast.ParseRes>): (std.ast.NodeFormat_Node, Int64) { /* compiled code */ }

func genNodeFormatNode(pNode: CPointer<UInt8>, pTokensBytes: CPointer<UInt8>): std.ast.NodeFormat_Node { /* compiled code */ }

func getTokensFromBytes(pTokenArray: CPointer<UInt8>): std.ast.Tokens { /* compiled code */ }

func cangjieLex(code: std.core.String): std.ast.Tokens { /* compiled code */ }

func cangjieLex(code: std.core.String, truncated: Bool): std.ast.Tokens { /* compiled code */ }

func unsafePointerCastFromUint8Array(arr: std.core.Array<UInt8>): CPointer<UInt8> { /* compiled code */ }

class MacroContextException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func assertParentContext(parentMacroName: std.core.String): Unit { /* compiled code */ }

func insideParentContext(parentMacroName: std.core.String): Bool { /* compiled code */ }

let ITEM_TYPE_STRING: UInt8

let ITEM_TYPE_INT64: UInt8

let ITEM_TYPE_BOOL: UInt8

let ITEM_TYPE_TOKENS: UInt8

func setItem(key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

func setItem(key: std.core.String, value: Int64): Unit { /* compiled code */ }

func setItem(key: std.core.String, value: Bool): Unit { /* compiled code */ }

func setItem(key: std.core.String, value: std.ast.Tokens): Unit { /* compiled code */ }

class McInfo {
    var keyVal: CString

    var value_: CPointer<Unit>

    var valueType_: UInt8

    init(keyVal_: CString, v: CPointer<Unit>, vType: UInt8) { /* compiled code */ }

    prop keyV: CString {
        get() { /* compiled code */ }
    }

    prop value: CPointer<Unit> {
        get() { /* compiled code */ }
    }

    prop valueType: UInt8 {
        get() { /* compiled code */ }
    }
}

class MacroMessage {
    var infoMap: std.collection.ArrayList<std.ast.McInfo>

    init(res: std.collection.ArrayList<std.ast.McInfo>) { /* compiled code */ }

    func hasItem(key: std.core.String): Bool { /* compiled code */ }

    func getString(key: std.core.String): std.core.String { /* compiled code */ }

    func getInt64(key: std.core.String): Int64 { /* compiled code */ }

    func getBool(key: std.core.String): Bool { /* compiled code */ }

    func getTokens(key: std.core.String): std.ast.Tokens { /* compiled code */ }
}

func readCpointerToMap(cp: CPointer<CPointer<Unit>>): std.collection.ArrayList<std.ast.McInfo> { /* compiled code */ }

func getChildMessages(children: std.core.String): std.collection.ArrayList<std.ast.MacroMessage> { /* compiled code */ }

class Table {
    var bytes: std.core.Array<UInt8>

    var pos: UInt32

    init(buf: std.core.Array<UInt8>, position: UInt32) { /* compiled code */ }

    func offset(slot: UInt16): UInt16 { /* compiled code */ }

    func getIndirect(offsetVal: UInt32): UInt32 { /* compiled code */ }

    func getByteVector(offsetVal: UInt32): std.core.Array<UInt8> { /* compiled code */ }

    func getVectorLenBySlot(slot: UInt16): Int64 { /* compiled code */ }

    func getVectorStartBySlot(slot: UInt16): UInt32 { /* compiled code */ }

    func getString(offsetVal: UInt32): std.core.String { /* compiled code */ }

    func getUInt8(offsetVal: UInt32): UInt8 { /* compiled code */ }

    func getInt8(offsetVal: UInt32): Int8 { /* compiled code */ }

    func getBool(offsetVal: UInt32): Bool { /* compiled code */ }

    func getUInt16(offsetVal: UInt32): UInt16 { /* compiled code */ }

    func getInt16(offsetVal: UInt32): Int16 { /* compiled code */ }

    func getUInt32(offsetVal: UInt32): UInt32 { /* compiled code */ }

    func getInt32(offsetVal: UInt32): Int32 { /* compiled code */ }

    func getUInt64(offsetVal: UInt32): UInt64 { /* compiled code */ }

    func getInt64(offsetVal: UInt32): Int64 { /* compiled code */ }

    func getUInt8Slot(slot: UInt16, default: UInt8): UInt8 { /* compiled code */ }

    func getInt8Slot(slot: UInt16, default: Int8): Int8 { /* compiled code */ }

    func getBoolSlot(slot: UInt16, default: Bool): Bool { /* compiled code */ }

    func getUInt16Slot(slot: UInt16, default: UInt16): UInt16 { /* compiled code */ }

    func getInt16Slot(slot: UInt16, default: Int16): Int16 { /* compiled code */ }

    func getUInt32Slot(slot: UInt16, default: UInt32): UInt32 { /* compiled code */ }

    func getInt32Slot(slot: UInt16, default: Int32): Int32 { /* compiled code */ }

    func getUInt64Slot(slot: UInt16, default: UInt64): UInt64 { /* compiled code */ }

    func getInt64Slot(slot: UInt16, default: Int64): Int64 { /* compiled code */ }
}

struct Position <: std.ast.ToBytes {
    let fileID: UInt32

    let line: Int32

    let column: Int32

    init() { /* compiled code */ }

    init(fileID: UInt32, line: Int32, column: Int32) { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    operator func ==(r: std.ast.Position): Bool { /* compiled code */ }

    operator func !=(r: std.ast.Position): Bool { /* compiled code */ }

    func dump(): Unit { /* compiled code */ }

    func toBytes(): std.core.Array<UInt8> { /* compiled code */ }

    func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64 { /* compiled code */ }

    func getSize(): Int64 { /* compiled code */ }
}

interface ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

class Program <: std.ast.Node {
    init(pkgHeader: std.ast.PackageHeader, importLists: std.collection.ArrayList<std.ast.ImportList>, decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var pkgHeader_: std.ast.PackageHeader

    var importLists_: std.collection.ArrayList<std.ast.ImportList>

    var decls_: std.collection.ArrayList<std.ast.Decl>

    mut prop packageHeader: std.ast.PackageHeader {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop importLists: std.collection.ArrayList<std.ast.ImportList> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop decls: std.collection.ArrayList<std.ast.Decl> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class PackageHeader <: std.ast.Node {
    init(accessibleToken: std.ast.Token, macroToken: std.ast.Token, keyword: std.ast.Token, prefixPaths: std.ast.Tokens, prefixDots: std.ast.Tokens, pkgIdentifier: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(isFromDerivation: Bool) { /* compiled code */ }

    var accessibleToken_: std.ast.Token

    var macroToken_: std.ast.Token

    var keyword_: std.ast.Token

    var prefixPaths_: std.ast.Tokens

    var prefixDots_: std.ast.Tokens

    var pkgIdentifier_: std.ast.Token

    var isFromDerivation_: Bool

    mut prop accessible: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordM: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop keywordP: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop prefixPaths: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop prefixDots: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop packageIdentifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }

    func addPackageTokens(input: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }
}

enum ImportKind <: std.core.ToString {
    | Single

    | Alias

    | All

    | Multi

    func toString(): std.core.String { /* compiled code */ }
}

class ImportContent <: std.ast.Node {
    init(importKind: std.ast.ImportKind, prefixPaths: std.ast.Tokens, prefixDots: std.ast.Tokens, identifier: std.ast.Token, importAlias: std.ast.Tokens, leftCurl: std.ast.Token, items: std.collection.ArrayList<std.ast.ImportContent>, commas: std.ast.Tokens, rightCurl: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var importKind_: std.ast.ImportKind

    var prefixPaths_: std.ast.Tokens

    var prefixDots_: std.ast.Tokens

    var identifier_: std.ast.Token

    var importAlias_: std.ast.Tokens

    var leftCurl_: std.ast.Token

    var items_: std.collection.ArrayList<std.ast.ImportContent>

    var commas_: std.ast.Tokens

    var rightCurl_: std.ast.Token

    let innerToken_: std.ast.Tokens

    mut prop importKind: std.ast.ImportKind {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop prefixPaths: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop prefixDots: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop importAlias: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop items: std.collection.ArrayList<std.ast.ImportContent> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rBrace: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func isImportAlias(): Bool { /* compiled code */ }

    func isImportAll(): Bool { /* compiled code */ }

    func isImportMulti(): Bool { /* compiled code */ }

    func isImportSingle(): Bool { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

extend ArrayList <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Array <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

func parseDecl(input: std.ast.Tokens, astKind: std.core.String): std.ast.Decl { /* compiled code */ }

func parseDeclFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Decl, Int64) { /* compiled code */ }

func generateDeclAST(declNode: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Decl { /* compiled code */ }

func createDeclBase(keyWord: std.ast.Token, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl { /* compiled code */ }

func generateGenericInfo(generic: std.core.Option<std.ast.NodeFormat_Generic>): (std.core.Option<std.ast.GenericParam>, std.collection.ArrayList<std.ast.GenericConstraint>, std.ast.Tokens, Bool) { /* compiled code */ }

func createClassDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ClassDecl { /* compiled code */ }

func createStrucDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.StructDecl { /* compiled code */ }

func createInterfaceDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.InterfaceDecl { /* compiled code */ }

func createEnumDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.EnumDecl { /* compiled code */ }

func createExtendDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ExtendDecl { /* compiled code */ }

func createFuncDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl { /* compiled code */ }

func createFuncParam(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

func createFuncParam(param: std.core.Option<std.ast.NodeFormat_FuncParam>, index: Int64, length: Int64): std.ast.FuncParam { /* compiled code */ }

func generateFuncParams(funcBody: std.ast.NodeFormat_FuncBody): std.collection.ArrayList<std.ast.FuncParam> { /* compiled code */ }

func generateCommonFuncDecl(funcDecl: std.core.Option<std.ast.NodeFormat_FuncDecl>, declNode: std.ast.Decl): std.ast.FuncDecl { /* compiled code */ }

func createFuncBody(funcBody: std.core.Option<std.ast.NodeFormat_FuncBody>): (std.collection.ArrayList<std.ast.FuncParam>, std.ast.Token, std.core.Option<std.ast.TypeNode>, std.ast.Block, std.ast.Token, std.ast.Token) { /* compiled code */ }

func createMainDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MainDecl { /* compiled code */ }

func createMacroDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MacroDecl { /* compiled code */ }

func createPrimaryCtorDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.PrimaryCtorDecl { /* compiled code */ }

func createVarDeclNode(decl: std.core.Option<std.ast.NodeFormat_VarDecl>): std.ast.VarDecl { /* compiled code */ }

func createVarDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

func createVarWithPatternDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

func createPropDecl(decl: std.ast.NodeFormat_Decl): std.ast.PropDecl { /* compiled code */ }

func createTypeAliasDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.TypeAliasDecl { /* compiled code */ }

func createMacroExpandDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl { /* compiled code */ }

func parseExpr(input: std.ast.Tokens): std.ast.Expr { /* compiled code */ }

func parseExprFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Expr, Int64) { /* compiled code */ }

func generateExprAST(exprNode: std.core.Option<std.ast.NodeFormat_Expr>): std.ast.Expr { /* compiled code */ }

func createBlock(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func generateBlock(blockOption: std.core.Option<std.ast.NodeFormat_Block>): std.ast.Block { /* compiled code */ }

func createBinaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createUnaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createIsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createAsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createParenExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createLitConstExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createCallExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createRefExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func generateRefExpr(ref: std.core.Option<std.ast.NodeFormat_RefExpr>): std.ast.RefExpr { /* compiled code */ }

func createReturnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createThrowExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createAssignExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createMemberAccess(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createOptionalExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createOptionalChainExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createIfExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createLetPatternExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.LetPatternExpr { /* compiled code */ }

func generateMatchCase(mc: std.core.Option<std.ast.NodeFormat_MatchCase>): std.ast.MatchCase { /* compiled code */ }

func generateMatchCaseOther(mco: std.core.Option<std.ast.NodeFormat_MatchCaseOther>): std.ast.MatchCase { /* compiled code */ }

func createMatchExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createDoWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createLambdaExpr(lambdaExpr: std.ast.NodeFormat_LambdaExpr): std.ast.LambdaExpr { /* compiled code */ }

func createLambdaExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createSpawnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createSynchronizedExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createTrailingClosureExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createTypeConvExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createForInExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createPrimitiveTypeExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createArrayLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createTupleLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createRangeExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createSubscriptExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createJumpExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createIncOrDecExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createTryExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createQuoteExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createQuoteToken(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createWildcardExpr(exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr { /* compiled code */ }

func createArrayExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

func createMacroExpandExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr { /* compiled code */ }

let EXPR_LIST: std.core.Array<std.core.String>

let DECL_LIST: std.core.Array<std.core.String>

let TYPE_LIST: std.core.Array<std.core.String>

let PATTERN_LIST: std.core.Array<std.core.String>

func parseProgram(input: std.ast.Tokens): std.ast.Program { /* compiled code */ }

func addPositionInfo(node: std.ast.Node, nodeBase: std.ast.NodeFormat_NodeBase): Unit { /* compiled code */ }

func generateNode(n: std.core.Option<std.ast.NodeFormat_Node>): std.ast.Node { /* compiled code */ }

func createAnnotation(anno: std.core.Option<std.ast.NodeFormat_Annotation>): std.ast.Annotation { /* compiled code */ }

func createModifier(modifier: std.core.Option<std.ast.NodeFormat_Modifier>): std.ast.Modifier { /* compiled code */ }

func createArgument(arg: std.core.Option<std.ast.NodeFormat_FuncArg>): std.ast.Argument { /* compiled code */ }

func generateGenericParamTk(gp: std.core.Option<std.ast.NodeFormat_GenericParamDecl>): (std.ast.Token, std.ast.Token) { /* compiled code */ }

func createGenericParam(gic: std.core.Option<std.ast.NodeFormat_Generic>): std.core.Option<std.ast.GenericParam> { /* compiled code */ }

func createGenericConstraint(flag: Bool, cons: std.core.Option<std.ast.NodeFormat_GenericConstraint>): std.ast.GenericConstraint { /* compiled code */ }

func createMacroExpand(nodeInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>, isDecl: Bool, isFuncParam: Bool): std.ast.Node { /* compiled code */ }

func processCustomAnnotationLikeMacro(macroInputDecl: std.core.Option<std.ast.Decl>, macroNode: std.ast.Decl): std.ast.Decl { /* compiled code */ }

func getPackageIdentToken(s: std.core.String): std.ast.Token { /* compiled code */ }

func getPackagePrefixes(prefixPaths: std.core.Array<std.core.Option<std.core.String>>, prefixPoses: std.core.Array<std.ast.NodeFormat_Position>, prefixDotPoses: std.core.Array<std.ast.NodeFormat_Position>): (std.ast.Tokens, std.ast.Tokens) { /* compiled code */ }

func isSamePos(pos1: std.ast.NodeFormat_Position, pos2: std.ast.NodeFormat_Position): Bool { /* compiled code */ }

func createPackageHeader(pkgHeader: std.core.Option<std.ast.NodeFormat_PackageSpec>): std.ast.PackageHeader { /* compiled code */ }

func createImportContent(content: std.core.Option<std.ast.NodeFormat_ImportContent>): std.ast.ImportContent { /* compiled code */ }

func createImportList(imports: std.core.Option<std.ast.NodeFormat_ImportSpec>): std.ast.ImportList { /* compiled code */ }

func createProgram(file: std.core.Option<std.ast.NodeFormat_File>): std.ast.Program { /* compiled code */ }

func createConstructor(decl: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Constructor { /* compiled code */ }

func createArgumentTokens(m: std.ast.Decl): std.ast.Tokens { /* compiled code */ }

func createCustomAnnotation(m: std.ast.Decl): std.ast.Annotation { /* compiled code */ }

func createConstructorLikeMacroExpand(decl: std.ast.NodeFormat_Decl): std.ast.Constructor { /* compiled code */ }

func createConstructorWithAt(decl: std.ast.Decl): std.ast.Constructor { /* compiled code */ }

func createConstructorWithAtExcl(decl: std.ast.Decl): std.ast.Constructor { /* compiled code */ }

func parsePattern(input: std.ast.Tokens): std.ast.Pattern { /* compiled code */ }

func parsePatternFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Pattern, Int64) { /* compiled code */ }

func generatePatternAST(patternNode: std.core.Option<std.ast.NodeFormat_Pattern>): std.ast.Pattern { /* compiled code */ }

func createVarPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.VarPattern { /* compiled code */ }

class VarPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createConstPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ConstPattern { /* compiled code */ }

class ConstPattern <: std.ast.Pattern {
    init(litConstExpr: std.ast.LitConstExpr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var litConstExpr_: std.ast.LitConstExpr

    mut prop litConstExpr: std.ast.LitConstExpr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createWildcardPattern(_: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.WildcardPattern { /* compiled code */ }

class WildcardPattern <: std.ast.Pattern {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    mut prop wildcard: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createVarOrEnumPattern(pattern: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.VarOrEnumPattern { /* compiled code */ }

class VarOrEnumPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createExceptTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ExceptTypePattern { /* compiled code */ }

class ExceptTypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, bitOr: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var pattern_: std.ast.Pattern

    var colon_: std.ast.Token

    var types_: std.collection.ArrayList<std.ast.TypeNode>

    var bitOr_: std.ast.Tokens

    mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TypePattern { /* compiled code */ }

class TypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var pattern_: std.ast.Pattern

    var colon_: std.ast.Token

    var type_: std.ast.TypeNode

    mut prop pattern: std.ast.Pattern {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop colon: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patternType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createEnumPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.EnumPattern { /* compiled code */ }

class EnumPattern <: std.ast.Pattern {
    init(constructor: std.ast.Expr, lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var constructor_: std.ast.Expr

    var lParen_: std.ast.Token

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    mut prop constructor: std.ast.Expr {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func createTuplePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TuplePattern { /* compiled code */ }

class TuplePattern <: std.ast.Pattern {
    init(lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var lParen_: std.ast.Token

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop patterns: std.collection.ArrayList<std.ast.Pattern> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

func parseType(input: std.ast.Tokens): std.ast.TypeNode { /* compiled code */ }

func parseTypeFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.TypeNode, Int64) { /* compiled code */ }

func generateTypeAST(tyNode: std.core.Option<std.ast.NodeFormat_Type>): std.ast.TypeNode { /* compiled code */ }

func createPrimitiveType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createRefType(tyNode: std.core.Option<std.ast.NodeFormat_RefType>, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createQualifiedType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createParenType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createTupleType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createThisType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createFuncType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createVArrayType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

func createPrefixType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode { /* compiled code */ }

interface ToBytes {
    func toBytes(): std.core.Array<UInt8> { /* compiled code */ }
}

func getMacroPosition(macroObjPtr: CPointer<Unit>): (UInt32, Int32, Int32) { /* compiled code */ }

let SIZE_OF_OFFSET: UInt32

let SIZE_OF_UINT32: UInt32

func castUInt32ToBytes(v: UInt32): std.core.Array<UInt8> { /* compiled code */ }

func castInt32ToBytes(v: Int32): std.core.Array<UInt8> { /* compiled code */ }

func castUInt16ToBytes(v: UInt16): std.core.Array<UInt8> { /* compiled code */ }

func castUInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt32): Int64 { /* compiled code */ }

func castInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: Int32): Int64 { /* compiled code */ }

func castUInt16ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt16): Int64 { /* compiled code */ }

func castStringToBytes(arr: std.core.Array<UInt8>, start1: Int64, s: std.core.String): Int64 { /* compiled code */ }

func concatBytes(l: std.core.Array<UInt8>, r: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

func castStringToBytes(s: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

func refreshPos(token: std.ast.Token, f: UInt32, l: Int32, c: Int32): std.ast.Token { /* compiled code */ }

let PATTERNS: std.core.Array<(std.ast.TokenKind, std.core.String, std.core.String)>

func getTokenID(tk: std.ast.TokenKind): UInt16 { /* compiled code */ }

func getTokenKind(no: UInt16): std.ast.TokenKind { /* compiled code */ }

func getNewlineValue(): std.core.String { /* compiled code */ }

func getTokenKindValue(tk: std.ast.TokenKind): std.core.String { /* compiled code */ }

func getTokenLiteral(tk: std.ast.TokenKind): std.core.String { /* compiled code */ }

class TokenKindHelper {
    let LIT_CONST_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    let STRING_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    let PRIMITIVE_TYPE_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    static func getPrimitiveTypeTokenKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    static func getLiteralConstKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    static func getStringKind(i: UInt16): std.ast.TokenKind { /* compiled code */ }

    init() { /* compiled code */ }
}

class TokensIterator <: std.core.Iterator<std.ast.Token> {
    var position: Int64

    let data: std.ast.Tokens

    init(tokens: std.ast.Tokens) { /* compiled code */ }

    func peek(): std.core.Option<std.ast.Token> { /* compiled code */ }

    func seeing(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    func next(): std.core.Option<std.ast.Token> { /* compiled code */ }
}

func refreshTokensPosition(tks: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

func tokenToString(tk: std.ast.Token): std.core.String { /* compiled code */ }

class ConcatTokens <: std.ast.Tokens {
    let leftChild: std.ast.Tokens

    var rightChild: std.ast.Tokens

    var cachedSize: Int64

    init(lc: std.ast.Tokens, rc: std.ast.Tokens) { /* compiled code */ }

    override prop size: Int64 {
        get() { /* compiled code */ }
    }

    override func `get`(index: Int64): std.ast.Token { /* compiled code */ }

    operator func [](range: std.core.Range<Int64>): std.ast.Tokens { /* compiled code */ }

    override func append(tks: std.ast.Tokens): std.ast.Tokens { /* compiled code */ }

    override func append(tk: std.ast.Token): std.ast.Tokens { /* compiled code */ }
}

extend Token <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Tokens <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

extend Extend <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

func countDelimiter(charArray: std.core.Array<Rune>): (UInt16, std.core.String, std.core.String) { /* compiled code */ }

extend String <: ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }
}

class RefType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, identifier: std.ast.Token, lAngle: std.ast.Token, typeArguments: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    var lAngle_: std.ast.Token

    var typeArguments_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rAngle_: std.ast.Token

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class QualifiedType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, baseType: std.ast.TypeNode, dot: std.ast.Token, identifier: std.ast.Token, lAngle: std.ast.Token, typeArguments: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rAngle: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var baseType_: std.ast.TypeNode

    var dot_: std.ast.Token

    var identifier_: std.ast.Token

    var lAngle_: std.ast.Token

    var typeArguments_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rAngle_: std.ast.Token

    mut prop baseType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop dot: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop identifier: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop typeArguments: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rAngle: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ParenType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, parenthesizedType: std.ast.TypeNode, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var lParen_: std.ast.Token

    var ty_: std.ast.TypeNode

    var rParen_: std.ast.Token

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop parenthesizedType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class TupleType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var types_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class ThisType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class PrefixType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, prefixOps: std.ast.Tokens, baseType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var prefixOps_: std.ast.Tokens

    var baseType_: std.ast.TypeNode

    mut prop prefixOps: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop baseType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}

class FuncType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token, lParen: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token, arrow: std.ast.Token, returnType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var lParen_: std.ast.Token

    var types_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    var arrow_: std.ast.Token

    var retTy_: std.ast.TypeNode

    mut prop keyword: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop lParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop types: std.collection.ArrayList<std.ast.TypeNode> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop commas: std.ast.Tokens {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop rParen: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop arrow: std.ast.Token {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop returnType: std.ast.TypeNode {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func traverse(v: std.ast.Visitor): Unit { /* compiled code */ }

    func dump(indent: UInt16): std.core.String { /* compiled code */ }
}
