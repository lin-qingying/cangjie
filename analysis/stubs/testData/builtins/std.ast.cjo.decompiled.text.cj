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

func EnumValuesNodeFormat_AnyExpr(e: UInt8): std.ast.NodeFormat_AnyExpr

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

func EnumValuesNodeFormat_AnyPattern(e: UInt8): std.ast.NodeFormat_AnyPattern

enum NodeFormat_MacroParam {
    | MacroParam_NONE

    | MacroParam_MACRO_EXPAND_PARAM
}

func EnumValuesNodeFormat_MacroParam(e: UInt8): std.ast.NodeFormat_MacroParam

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

func EnumValuesNodeFormat_AnyType(e: UInt8): std.ast.NodeFormat_AnyType

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

func EnumValuesNodeFormat_AnyDecl(e: UInt8): std.ast.NodeFormat_AnyDecl

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

func EnumValuesNodeFormat_AnyNode(e: UInt8): std.ast.NodeFormat_AnyNode

enum NodeFormat_AccessibleKind {
    | AccessibleKind_ACCESSIBLE_PUBLIC

    | AccessibleKind_ACCESSIBLE_PROTECTED

    | AccessibleKind_ACCESSIBLE_INTERNAL
}

func EnumValuesNodeFormat_AccessibleKind(e: UInt8): std.ast.NodeFormat_AccessibleKind

enum NodeFormat_ImportKind {
    | ImportKind_IMPORT_SINGLE

    | ImportKind_IMPORT_ALIAS

    | ImportKind_IMPORT_ALL

    | ImportKind_IMPORT_MULTI
}

func EnumValuesNodeFormat_ImportKind(e: UInt8): std.ast.NodeFormat_ImportKind

enum NodeFormat_ReExportKind {
    | ReExportKind_REEXPORT_PRIVATE

    | ReExportKind_REEXPORT_INTERNAL

    | ReExportKind_REEXPORT_PROTECTED

    | ReExportKind_REEXPORT_PUBLIC
}

func EnumValuesNodeFormat_ReExportKind(e: UInt8): std.ast.NodeFormat_ReExportKind

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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPatterns: std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>>

    func GetPatternguard: std.core.Option<std.ast.NodeFormat_Expr>

    func GetIfPos: std.ast.NodeFormat_Position

    func GetArrowPos: std.ast.NodeFormat_Position

    func GetExprOrDecls: std.core.Option<std.ast.NodeFormat_Block>

    func GetBitOrPosVec: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_NodeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BEGIN: UInt16

    let END: UInt16

    let AST_KIND: UInt16

    func GetBegin: std.ast.NodeFormat_Position

    func GetEnd: std.ast.NodeFormat_Position

    func GetAstKind: std.core.String
}

class NodeFormat_Pattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN_TYPE: UInt16

    let PATTERN: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPatternType: std.ast.NodeFormat_AnyPattern

    func GetPatternAsConstPattern: std.core.Option<std.ast.NodeFormat_ConstPattern>

    func GetPatternAsWildcardPattern: std.core.Option<std.ast.NodeFormat_WildcardPattern>

    func GetPatternAsVarPattern: std.core.Option<std.ast.NodeFormat_VarPattern>

    func GetPatternAsExceptTypePattern: std.core.Option<std.ast.NodeFormat_ExceptTypePattern>

    func GetPatternAsTypePattern: std.core.Option<std.ast.NodeFormat_TypePattern>

    func GetPatternAsEnumPattern: std.core.Option<std.ast.NodeFormat_EnumPattern>

    func GetPatternAsVarOrEnumPattern: std.core.Option<std.ast.NodeFormat_VarOrEnumPattern>

    func GetPatternAsTuplePattern: std.core.Option<std.ast.NodeFormat_TuplePattern>

    func GetPatternAsCommandTypePattern: std.core.Option<std.ast.NodeFormat_CommandTypePattern>
}

class NodeFormat_Expr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR_TYPE: UInt16

    let EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExprType: std.ast.NodeFormat_AnyExpr

    func GetExprAsBinaryExpr: std.core.Option<std.ast.NodeFormat_BinaryExpr>

    func GetExprAsUnaryExpr: std.core.Option<std.ast.NodeFormat_UnaryExpr>

    func GetExprAsParenExpr: std.core.Option<std.ast.NodeFormat_ParenExpr>

    func GetExprAsLitConstExpr: std.core.Option<std.ast.NodeFormat_LitConstExpr>

    func GetExprAsCallExpr: std.core.Option<std.ast.NodeFormat_CallExpr>

    func GetExprAsRefExpr: std.core.Option<std.ast.NodeFormat_RefExpr>

    func GetExprAsReturnExpr: std.core.Option<std.ast.NodeFormat_ReturnExpr>

    func GetExprAsAssignExpr: std.core.Option<std.ast.NodeFormat_AssignExpr>

    func GetExprAsMemberAccess: std.core.Option<std.ast.NodeFormat_MemberAccess>

    func GetExprAsIfExpr: std.core.Option<std.ast.NodeFormat_IfExpr>

    func GetExprAsBlock: std.core.Option<std.ast.NodeFormat_Block>

    func GetExprAsLambdaExpr: std.core.Option<std.ast.NodeFormat_LambdaExpr>

    func GetExprAsArrayLit: std.core.Option<std.ast.NodeFormat_ArrayLit>

    func GetExprAsTupleLit: std.core.Option<std.ast.NodeFormat_TupleLit>

    func GetExprAsSubscriptExpr: std.core.Option<std.ast.NodeFormat_SubscriptExpr>

    func GetExprAsRangeExpr: std.core.Option<std.ast.NodeFormat_RangeExpr>

    func GetExprAsForInExpr: std.core.Option<std.ast.NodeFormat_ForInExpr>

    func GetExprAsIsExpr: std.core.Option<std.ast.NodeFormat_IsExpr>

    func GetExprAsAsExpr: std.core.Option<std.ast.NodeFormat_AsExpr>

    func GetExprAsTrailingClosureExpr: std.core.Option<std.ast.NodeFormat_TrailingClosureExpr>

    func GetExprAsTypeConvExpr: std.core.Option<std.ast.NodeFormat_TypeConvExpr>

    func GetExprAsThrowExpr: std.core.Option<std.ast.NodeFormat_ThrowExpr>

    func GetExprAsTryExpr: std.core.Option<std.ast.NodeFormat_TryExpr>

    func GetExprAsPrimitiveTypeExpr: std.core.Option<std.ast.NodeFormat_PrimitiveTypeExpr>

    func GetExprAsWhileExpr: std.core.Option<std.ast.NodeFormat_WhileExpr>

    func GetExprAsDoWhileExpr: std.core.Option<std.ast.NodeFormat_DoWhileExpr>

    func GetExprAsJumpExpr: std.core.Option<std.ast.NodeFormat_JumpExpr>

    func GetExprAsIncOrDecExpr: std.core.Option<std.ast.NodeFormat_IncOrDecExpr>

    func GetExprAsSpawnExpr: std.core.Option<std.ast.NodeFormat_SpawnExpr>

    func GetExprAsSynchronizedExpr: std.core.Option<std.ast.NodeFormat_SynchronizedExpr>

    func GetExprAsLetPatternDestructor: std.core.Option<std.ast.NodeFormat_LetPatternDestructor>

    func GetExprAsTokenPart: std.core.Option<std.ast.NodeFormat_TokenPart>

    func GetExprAsQuoteExpr: std.core.Option<std.ast.NodeFormat_QuoteExpr>

    func GetExprAsMatchExpr: std.core.Option<std.ast.NodeFormat_MatchExpr>

    func GetExprAsOptionalExpr: std.core.Option<std.ast.NodeFormat_OptionalExpr>

    func GetExprAsOptionalChainExpr: std.core.Option<std.ast.NodeFormat_OptionalChainExpr>

    func GetExprAsMacroExpandExpr: std.core.Option<std.ast.NodeFormat_MacroExpandExpr>

    func GetExprAsWildcardExpr: std.core.Option<std.ast.NodeFormat_WildcardExpr>

    func GetExprAsArrayExpr: std.core.Option<std.ast.NodeFormat_ArrayExpr>
}

class NodeFormat_Block <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let BODY: UInt16

    let RIGHT_CURL_POS: UInt16

    let IS_UNSAFE: UInt16

    let UNSAFE_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetBody: std.core.Array<std.core.Option<std.ast.NodeFormat_Node>>

    func GetRightCurlPos: std.ast.NodeFormat_Position

    func GetIsUnsafe: Bool

    func GetUnsafePos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetMatchExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetArrowPos: std.ast.NodeFormat_Position

    func GetExprOrDecls: std.core.Option<std.ast.NodeFormat_Block>
}

class NodeFormat_ConstPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LITERAL: UInt16

    let OPERATOR_CALL_EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLiteral: std.core.Option<std.ast.NodeFormat_Expr>

    func GetOperatorCallExpr: std.core.Option<std.ast.NodeFormat_CallExpr>
}

class NodeFormat_WildcardPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>
}

class NodeFormat_VarPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let VAR_DECL: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetVarDecl: std.core.Option<std.ast.NodeFormat_VarDecl>
}

class NodeFormat_ExceptTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let PATTERN_POS: UInt16

    let COLON_POS: UInt16

    let TYPES: UInt16

    let BIT_OR_POS_VEC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetPatternPos: std.ast.NodeFormat_Position

    func GetColonPos: std.ast.NodeFormat_Position

    func GetTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetBitOrPosVec: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_TypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let COLON_POS: UInt16

    let TYPE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetType: std.core.Option<std.ast.NodeFormat_Type>
}

class NodeFormat_EnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REF: UInt16

    let LEFT_PAREN_POS: UInt16

    let PATTERNS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VEC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetRef: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetPatterns: std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetCommaPosVec: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_VarOrEnumPattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IDENTIFIER: UInt16

    let PATTERN: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetIdentifier: std.core.String

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>
}

class NodeFormat_TuplePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PATTERNS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VEC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetPatterns: std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetCommaPosVec: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_CommandTypePattern <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERN: UInt16

    let PATTERN_POS: UInt16

    let COLON_POS: UInt16

    let TYPES: UInt16

    let BIT_OR_POS_VEC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetPatternPos: std.ast.NodeFormat_Position

    func GetColonPos: std.ast.NodeFormat_Position

    func GetTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetBitOrPosVec: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_CallExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_FUNC: UInt16

    let LEFT_PAREN_POS: UInt16

    let ARGS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBaseFunc: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetArgs: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>>

    func GetRightParenPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetInitializer: std.core.Option<std.ast.NodeFormat_Expr>

    func GetAssignPos: std.ast.NodeFormat_Position

    func GetIsVar: Bool

    func GetIsEnumConstruct: Bool

    func GetEmptyKeyword: Bool
}

class NodeFormat_Type <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE_TYPE: UInt16

    let TYPE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetTypeType: std.ast.NodeFormat_AnyType

    func GetTypeAsRefType: std.core.Option<std.ast.NodeFormat_RefType>

    func GetTypeAsPrimitiveType: std.core.Option<std.ast.NodeFormat_PrimitiveType>

    func GetTypeAsFuncType: std.core.Option<std.ast.NodeFormat_FuncType>

    func GetTypeAsThisType: std.core.Option<std.ast.NodeFormat_ThisType>

    func GetTypeAsParenType: std.core.Option<std.ast.NodeFormat_ParenType>

    func GetTypeAsQualifiedType: std.core.Option<std.ast.NodeFormat_QualifiedType>

    func GetTypeAsOptionType: std.core.Option<std.ast.NodeFormat_OptionType>

    func GetTypeAsTupleType: std.core.Option<std.ast.NodeFormat_TupleType>

    func GetTypeAsVArrayType: std.core.Option<std.ast.NodeFormat_VArrayType>

    func GetTypeAsConstantType: std.core.Option<std.ast.NodeFormat_ConstantType>
}

class NodeFormat_BinaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_EXPR: UInt16

    let RIGHT_EXPR: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetOperatorKind: UInt16

    func GetOperatorPos: std.ast.NodeFormat_Position
}

class NodeFormat_IsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let IS_TYPE: UInt16

    let IS_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetIsType: std.core.Option<std.ast.NodeFormat_Type>

    func GetIsPos: std.ast.NodeFormat_Position
}

class NodeFormat_AsExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let AS_TYPE: UInt16

    let AS_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetAsType: std.core.Option<std.ast.NodeFormat_Type>

    func GetAsPos: std.ast.NodeFormat_Position
}

class NodeFormat_UnaryExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetOperatorKind: UInt16

    func GetOperatorPos: std.ast.NodeFormat_Position
}

class NodeFormat_WildcardExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>
}

class NodeFormat_ArrayExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let LEFTPARENPOS: UInt16

    let ARGS: UInt16

    let RIGHTPARENPOS: UInt16

    let ISVALUEARRAY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetArgs: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetIsValueArray: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetName: std.core.String

    func GetNamePos: std.ast.NodeFormat_Position

    func GetColonPos: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetCommaPos: std.ast.NodeFormat_Position

    func GetWithInout: Bool
}

class NodeFormat_ParenExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_LitConstExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LITERAL: UInt16

    let LITERAL_CONST_KIND: UInt16

    let DELIMITER_NUM: UInt16

    let STRING_KIND: UInt16

    let IS_SINGLE_QUOTE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLiteral: std.core.String

    func GetLiteralConstKind: UInt16

    func GetDelimiterNum: UInt16

    func GetStringKind: UInt16

    func GetIsSingleQuote: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetRef: std.core.Option<std.ast.NodeFormat_Reference>

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeArguments: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetRightAnglePos: std.ast.NodeFormat_Position

    func GetIsThis: Bool

    func GetIsSuper: Bool

    func GetIsQuoteDollar: Bool
}

class NodeFormat_Reference <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let IDENTIFIER: UInt16

    let IDENTIFIER_POS: UInt16

    func GetIdentifier: std.core.String

    func GetIdentifierPos: std.ast.NodeFormat_Position
}

class NodeFormat_ReturnExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let RETURN_POS: UInt16

    let EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetReturnPos: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>
}

class NodeFormat_AssignExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_VALUE: UInt16

    let ASSIGN_OP: UInt16

    let ASSIGN_POS: UInt16

    let RIGHT_EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftValue: std.core.Option<std.ast.NodeFormat_Expr>

    func GetAssignOp: UInt16

    func GetAssignPos: std.ast.NodeFormat_Position

    func GetRightExpr: std.core.Option<std.ast.NodeFormat_Expr>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBaseExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetDotPos: std.ast.NodeFormat_Position

    func GetField: std.core.String

    func GetFieldPos: std.ast.NodeFormat_Position

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeArguments: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetRightAnglePos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetIfPos: std.ast.NodeFormat_Position

    func GetCondExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>

    func GetHasElse: Bool

    func GetElsePos: std.ast.NodeFormat_Position

    func GetElseBody: std.core.Option<std.ast.NodeFormat_Expr>

    func GetIsElseIf: Bool

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_LetPatternDestructor <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let PATTERNS: UInt16

    let BIT_OR_POS_VEC: UInt16

    let BACKARROW_POS: UInt16

    let INITIALIZER: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetPatterns: std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>>

    func GetBitOrPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetBackarrowPos: std.ast.NodeFormat_Position

    func GetInitializer: std.core.Option<std.ast.NodeFormat_Expr>
}

class NodeFormat_LambdaExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BODY: UInt16

    let MOCKSUPPORTED: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBody: std.core.Option<std.ast.NodeFormat_FuncBody>

    func GetMockSupported: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetParamList: std.core.Option<std.ast.NodeFormat_FuncParamList>

    func GetArrowPos: std.ast.NodeFormat_Position

    func GetColonPos: std.ast.NodeFormat_Position

    func GetRetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetHasBody: Bool

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>

    func GetGeneric: std.core.Option<std.ast.NodeFormat_Generic>
}

class NodeFormat_ArrayLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let CHILDREN: UInt16

    let COMMA_POS_VEC: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetChildren: std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>>

    func GetCommaPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

class NodeFormat_TupleLit <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let CHILDREN: UInt16

    let COMMA_POS_VEC: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetChildren: std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>>

    func GetCommaPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_SubscriptExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASE_EXPR: UInt16

    let LEFT_SQUARE_POS: UInt16

    let INDEX_EXPRS: UInt16

    let RIGHT_SQUARE_POS: UInt16

    let IS_TUPLE_ACCESS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBaseExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLeftSquarePos: std.ast.NodeFormat_Position

    func GetIndexExprs: std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>>

    func GetRightSquarePos: std.ast.NodeFormat_Position

    func GetIsTupleAccess: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetMatchMode: Bool

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetSelector: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetMatchCases: std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCase>>

    func GetMatchCaseOthers: std.core.Array<std.core.Option<std.ast.NodeFormat_MatchCaseOther>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetStartExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRangePos: std.ast.NodeFormat_Position

    func GetStopExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetStepExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetIsClosed: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetInPos: std.ast.NodeFormat_Position

    func GetInExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetIfPos: std.ast.NodeFormat_Position

    func GetPatternGuard: std.core.Option<std.ast.NodeFormat_Expr>

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>
}

class NodeFormat_WhileExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let WHILE_POS: UInt16

    let LEFT_PAREN_POS: UInt16

    let COND_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let BODY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetWhilePos: std.ast.NodeFormat_Position

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetCondExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetSpawnPos: std.ast.NodeFormat_Position

    func GetTaskExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetHasArg: Bool

    func GetSpawnArgExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_SynchronizedExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let SYNC_POS: UInt16

    let LEFT_PAREN_POS: UInt16

    let MUTEX_EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    let BODY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetSyncPos: std.ast.NodeFormat_Position

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetMutexExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>
}

class NodeFormat_TrailingClosureExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_LAMBDA: UInt16

    let EXPR: UInt16

    let LAMBDA: UInt16

    let RIGHT_LAMBDA: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftLambda: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLambda: std.core.Option<std.ast.NodeFormat_LambdaExpr>

    func GetRightLambda: std.ast.NodeFormat_Position
}

class NodeFormat_TypeConvExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPR: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_ThrowExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetResourceSpec: std.core.Array<std.core.Option<std.ast.NodeFormat_VarDecl>>

    func GetIsResourceSpec: Bool

    func GetTryBlock: std.core.Option<std.ast.NodeFormat_Block>

    func GetCatchBlocks: std.core.Array<std.core.Option<std.ast.NodeFormat_Block>>

    func GetCatchPatterns: std.core.Array<std.core.Option<std.ast.NodeFormat_Pattern>>

    func GetFinallyPos: std.ast.NodeFormat_Position

    func GetFinallyBlock: std.core.Option<std.ast.NodeFormat_Block>

    func GetResourceSpecLparenPos: std.ast.NodeFormat_Position

    func GetResourceSpecRparenPos: std.ast.NodeFormat_Position

    func GetResourceSpecCommaPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetCatchPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetCatchLeftParenPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetCatchRightParenPosVec: std.core.Array<std.ast.NodeFormat_Position>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetDoPos: std.ast.NodeFormat_Position

    func GetBody: std.core.Option<std.ast.NodeFormat_Block>

    func GetWhilePos: std.ast.NodeFormat_Position

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetCondExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_IncOrDecExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let OPERATOR_KIND: UInt16

    let OPERATOR_POS: UInt16

    let EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetOperatorKind: UInt16

    func GetOperatorPos: std.ast.NodeFormat_Position

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>
}

class NodeFormat_OptionalExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BASEEXPR: UInt16

    let QUEST_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBaseExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetQuestPos: std.ast.NodeFormat_Position
}

class NodeFormat_OptionalChainExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let EXPR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetExpr: std.core.Option<std.ast.NodeFormat_Expr>
}

class NodeFormat_Token <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let KIND: UInt16

    let VALUE: UInt16

    let POS: UInt16

    func GetKind: UInt16

    func GetValue: std.core.String

    func GetPos: std.ast.NodeFormat_Position
}

class NodeFormat_TokenPart <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let TOKENS: UInt16

    func GetTokens: std.core.Array<std.core.Option<std.ast.NodeFormat_Token>>
}

class NodeFormat_QuoteExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let EXPRS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetExprs: std.core.Array<std.core.Option<std.ast.NodeFormat_Expr>>

    func GetRightParenPos: std.ast.NodeFormat_Position
}

class NodeFormat_FuncParamList <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let PARAMS: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetParams: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncParam>>

    func GetRightParenPos: std.ast.NodeFormat_Position
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

    func GetNodeBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetBase: std.core.Option<std.ast.NodeFormat_VarDecl>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetAssignment: std.core.Option<std.ast.NodeFormat_Expr>

    func GetCommaPos: std.ast.NodeFormat_Position

    func GetIsNamedParam: Bool

    func GetIsMemberParam: Bool

    func GetNotMarkPos: std.ast.NodeFormat_Position

    func GetHasLetOrVar: Bool

    func GetMacroParamType: std.ast.NodeFormat_MacroParam

    func GetMacroParamAsMacroExpandParam: std.core.Option<std.ast.NodeFormat_MacroExpandParam>
}

class NodeFormat_MacroExpandParam <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_FuncParam>

    func GetInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>
}

class NodeFormat_Generic <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let LEFT_ANGLE_POS: UInt16

    let TYPE_PARAMETERS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    let GENERIC_CONSTRAINTS: UInt16

    let CONTENT: UInt16

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeParameters: std.core.Array<std.core.Option<std.ast.NodeFormat_GenericParamDecl>>

    func GetRightAnglePos: std.ast.NodeFormat_Position

    func GetGenericConstraints: std.core.Array<std.core.Option<std.ast.NodeFormat_GenericConstraint>>

    func GetContent: std.core.String
}

class NodeFormat_JumpExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let IS_BREAK: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetIsBreak: Bool
}

class NodeFormat_PrimitiveTypeExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetKind: UInt16
}

class NodeFormat_MacroExpandExpr <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    let IDENTIFIER: UInt16

    let IDENTIFIERPOS: UInt16

    let ANNOTATIONS: UInt16

    let MODIFIERS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>

    func GetIdentifier: std.core.String

    func GetIdentifierPos: std.ast.NodeFormat_Position

    func GetAnnotations: std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>>

    func GetModifiers: std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>>
}

class NodeFormat_TypeBase <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMMA_POS: UInt16

    let TYPE_PARAMETER_NAME: UInt16

    let COLON_POS: UInt16

    let TYPE_POS: UInt16

    let BIT_AND_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetCommaPos: std.ast.NodeFormat_Position

    func GetTypeParameterName: std.core.String

    func GetColonPos: std.ast.NodeFormat_Position

    func GetTypePos: std.ast.NodeFormat_Position

    func GetBitAndPos: std.ast.NodeFormat_Position
}

class NodeFormat_RefType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REF: UInt16

    let LEFT_ANGLE_POS: UInt16

    let TYPE_ARGUMENTS: UInt16

    let RIGHT_ANGLE_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetRef: std.core.Option<std.ast.NodeFormat_Reference>

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeArguments: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetRightAnglePos: std.ast.NodeFormat_Position
}

class NodeFormat_PrimitiveType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let TYPE_STR: UInt16

    let KIND: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetTypeStr: std.core.String

    func GetKind: UInt16
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

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetParamTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetArrowPos: std.ast.NodeFormat_Position

    func GetRetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetIsC: Bool
}

class NodeFormat_ThisType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>
}

class NodeFormat_ParenType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_PAREN_POS: UInt16

    let TYPE: UInt16

    let RIGHT_PAREN_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetRightParenPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetBaseType: std.core.Option<std.ast.NodeFormat_Type>

    func GetDotPos: std.ast.NodeFormat_Position

    func GetField: std.core.String

    func GetFieldPos: std.ast.NodeFormat_Position

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeArguments: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetRightAnglePos: std.ast.NodeFormat_Position
}

class NodeFormat_OptionType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMPONENT_TYPE: UInt16

    let QUEST_NUM: UInt16

    let QUEST_VECTOR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetComponentType: std.core.Option<std.ast.NodeFormat_Type>

    func GetQuestNum: Int32

    func GetQuestVector: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_TupleType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FIELD_TYPES: UInt16

    let LEFT_PAREN_POS: UInt16

    let RIGHT_PAREN_POS: UInt16

    let COMMA_POS_VECTOR: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetFieldTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetCommaPosVector: std.core.Array<std.ast.NodeFormat_Position>
}

class NodeFormat_VArrayType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let VARRAYPOS: UInt16

    let LEFTANGLEPOS: UInt16

    let TYPEARGUMENT: UInt16

    let CONSTANTTYPE: UInt16

    let RIGHTANGLEPOS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetVarrayPos: std.ast.NodeFormat_Position

    func GetLeftAnglePos: std.ast.NodeFormat_Position

    func GetTypeArgument: std.core.Option<std.ast.NodeFormat_Type>

    func GetConstantType: std.core.Option<std.ast.NodeFormat_Type>

    func GetRightAnglePos: std.ast.NodeFormat_Position
}

class NodeFormat_ConstantType <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let CONSTANTEXPR: UInt16

    let DOLLARPOS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_TypeBase>

    func GetConstantExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetDollarPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetIdentifier: std.core.String

    func GetIdentifierPos: std.ast.NodeFormat_Position

    func GetKeywordPos: std.ast.NodeFormat_Position

    func GetAnnotations: std.core.Array<std.core.Option<std.ast.NodeFormat_Annotation>>

    func GetModifiers: std.core.Array<std.core.Option<std.ast.NodeFormat_Modifier>>

    func GetGeneric: std.core.Option<std.ast.NodeFormat_Generic>

    func GetIsConst: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetInitializer: std.core.Option<std.ast.NodeFormat_Expr>

    func GetPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetAssignPos: std.ast.NodeFormat_Position

    func GetIsVar: Bool
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

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetFuncBody: std.core.Option<std.ast.NodeFormat_FuncBody>

    func GetIsSetter: Bool

    func GetIsGetter: Bool

    func GetOpKind: Int32

    func GetIsEnumConstruct: Bool
}

class NodeFormat_MainDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FUNC_BODY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetFuncBody: std.core.Option<std.ast.NodeFormat_FuncBody>
}

class NodeFormat_MacroDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFTPARENPOS: UInt16

    let RIGHTPARENPOS: UInt16

    let FUNC_BODY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetFuncBody: std.core.Option<std.ast.NodeFormat_FuncBody>
}

class NodeFormat_StructBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

class NodeFormat_Decl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let DECL_TYPE: UInt16

    let DECL: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetDeclType: std.ast.NodeFormat_AnyDecl

    func GetDeclAsFuncDecl: std.core.Option<std.ast.NodeFormat_FuncDecl>

    func GetDeclAsMainDecl: std.core.Option<std.ast.NodeFormat_MainDecl>

    func GetDeclAsVarDecl: std.core.Option<std.ast.NodeFormat_VarDecl>

    func GetDeclAsVarWithPatternDecl: std.core.Option<std.ast.NodeFormat_VarWithPatternDecl>

    func GetDeclAsStructDecl: std.core.Option<std.ast.NodeFormat_StructDecl>

    func GetDeclAsClassDecl: std.core.Option<std.ast.NodeFormat_ClassDecl>

    func GetDeclAsInterfaceDecl: std.core.Option<std.ast.NodeFormat_InterfaceDecl>

    func GetDeclAsGenericParamDecl: std.core.Option<std.ast.NodeFormat_GenericParamDecl>

    func GetDeclAsPrimaryCtorDecl: std.core.Option<std.ast.NodeFormat_PrimaryCtorDecl>

    func GetDeclAsPropDecl: std.core.Option<std.ast.NodeFormat_PropDecl>

    func GetDeclAsEnumDecl: std.core.Option<std.ast.NodeFormat_EnumDecl>

    func GetDeclAsTypeAliasDecl: std.core.Option<std.ast.NodeFormat_TypeAliasDecl>

    func GetDeclAsExtendDecl: std.core.Option<std.ast.NodeFormat_ExtendDecl>

    func GetDeclAsMacroDecl: std.core.Option<std.ast.NodeFormat_MacroDecl>

    func GetDeclAsMacroExpandDecl: std.core.Option<std.ast.NodeFormat_MacroExpandDecl>

    func GetDeclAsFuncParam: std.core.Option<std.ast.NodeFormat_FuncParam>
}

class NodeFormat_StructDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let BODY: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let GENERIC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetBody: std.core.Option<std.ast.NodeFormat_StructBody>

    func GetUpperBoundPos: std.ast.NodeFormat_Position

    func GetSuperTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetGeneric: std.core.Option<std.ast.NodeFormat_Generic>
}

class NodeFormat_ClassBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

class NodeFormat_ClassDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let BODY: UInt16

    let SUB_DECLS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetUpperBoundPos: std.ast.NodeFormat_Position

    func GetSuperTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetBody: std.core.Option<std.ast.NodeFormat_ClassBody>

    func GetSubDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>
}

class NodeFormat_InterfaceBody <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let LEFT_CURL_POS: UInt16

    let DECLS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

class NodeFormat_InterfaceDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let UPPER_BOUND_POS: UInt16

    let SUPER_TYPES: UInt16

    let BODY: UInt16

    let SUB_DECLS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetUpperBoundPos: std.ast.NodeFormat_Position

    func GetSuperTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetBody: std.core.Option<std.ast.NodeFormat_InterfaceBody>

    func GetSubDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetHasArguments: Bool

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetConstructors: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetBitOrPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetMembers: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position

    func GetUpperBoundPos: std.ast.NodeFormat_Position

    func GetSuperInterfaceTypes: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetHasEllipsis: Bool

    func GetEllipsisPos: std.ast.NodeFormat_Position
}

class NodeFormat_GenericParamDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COMMA_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetCommaPos: std.ast.NodeFormat_Position
}

class NodeFormat_PrimaryCtorDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FUNC_BODY: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetFuncBody: std.core.Option<std.ast.NodeFormat_FuncBody>
}

class NodeFormat_PropDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let COLON_POS: UInt16

    let LEFT_CURL_POS: UInt16

    let GETTERS: UInt16

    let SETTERS: UInt16

    let RIGHT_CURL_POS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_VarDecl>

    func GetColonPos: std.ast.NodeFormat_Position

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetGetters: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>>

    func GetSetters: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncDecl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetWherePos: std.ast.NodeFormat_Position

    func GetType: std.core.Option<std.ast.NodeFormat_RefType>

    func GetOperatorPos: std.ast.NodeFormat_Position

    func GetUpperBound: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetBitAndPosVec: std.core.Array<std.ast.NodeFormat_Position>

    func GetCommaPos: std.ast.NodeFormat_Position
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetKind: UInt16

    func GetIdentPos: std.ast.NodeFormat_Position

    func GetIdentifier: std.core.String

    func GetArgs: std.core.Array<std.core.Option<std.ast.NodeFormat_FuncArg>>

    func GetOverflowStrategy: std.core.String

    func GetAttrs: std.core.Array<std.core.Option<std.core.String>>

    func GetCondExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetLeftSquarePos: std.ast.NodeFormat_Position

    func GetRightSquarePos: std.ast.NodeFormat_Position

    func GetIsCompileTimeVisible: Bool
}

class NodeFormat_Modifier <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let KIND: UInt16

    let IS_EXPLICIT: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetKind: UInt16

    func GetIsExplicit: Bool
}

class NodeFormat_TypeAliasDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let ASSIGN_POS: UInt16

    let TYPE: UInt16

    let GENERIC: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetAssignPos: std.ast.NodeFormat_Position

    func GetType: std.core.Option<std.ast.NodeFormat_Type>

    func GetGeneric: std.core.Option<std.ast.NodeFormat_Generic>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetExtendedType: std.core.Option<std.ast.NodeFormat_Type>

    func GetUpperBoundPos: std.ast.NodeFormat_Position

    func GetInterfaces: std.core.Array<std.core.Option<std.ast.NodeFormat_Type>>

    func GetWherePos: std.ast.NodeFormat_Position

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetMembers: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

class NodeFormat_MacroExpandDecl <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let INVOCATION: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_DeclBase>

    func GetInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>
}

class NodeFormat_Node <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let ROOT_TYPE: UInt16

    let ROOT: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetRootType: std.ast.NodeFormat_AnyNode

    func GetRootAsExpr: std.core.Option<std.ast.NodeFormat_Expr>

    func GetRootAsDecl: std.core.Option<std.ast.NodeFormat_Decl>

    func GetRootAsType: std.core.Option<std.ast.NodeFormat_Type>

    func GetRootAsPattern: std.core.Option<std.ast.NodeFormat_Pattern>

    func GetRootAsFile: std.core.Option<std.ast.NodeFormat_File>

    func GetRootAsPackageSpec: std.core.Option<std.ast.NodeFormat_PackageSpec>

    func GetRootAsImportSpec: std.core.Option<std.ast.NodeFormat_ImportSpec>

    func GetRootAsAnnotation: std.core.Option<std.ast.NodeFormat_Annotation>
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

    func GetFullName: std.core.String

    func GetIdentifier: std.core.String

    func GetIdentifierPos: std.ast.NodeFormat_Position

    func GetLeftSquarePos: std.ast.NodeFormat_Position

    func GetRightSquarePos: std.ast.NodeFormat_Position

    func GetLeftParenPos: std.ast.NodeFormat_Position

    func GetRightParenPos: std.ast.NodeFormat_Position

    func GetAtPos: std.ast.NodeFormat_Position

    func GetAttrs: std.core.Array<std.core.Option<std.ast.NodeFormat_Token>>

    func GetArgs: std.core.Array<std.core.Option<std.ast.NodeFormat_Token>>

    func GetDecl: std.core.Option<std.ast.NodeFormat_Decl>

    func GetHasParenthesis: Bool

    func GetIsCompileTimeVisible: Bool
}

class NodeFormat_File <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let FILENAME: UInt16

    let FILEPATH: UInt16

    let PACKAGE: UInt16

    let IMPORTS: UInt16

    let DECLS: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetFileName: std.core.String

    func GetFilePath: std.core.String

    func GetPackage: std.core.Option<std.ast.NodeFormat_PackageSpec>

    func GetImports: std.core.Array<std.core.Option<std.ast.NodeFormat_ImportSpec>>

    func GetDecls: std.core.Array<std.core.Option<std.ast.NodeFormat_Decl>>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetMacroPos: std.ast.NodeFormat_Position

    func GetPackagePos: std.ast.NodeFormat_Position

    func GetPrefixPaths: std.core.Array<std.core.Option<std.core.String>>

    func GetPrefixPoses: std.core.Array<std.ast.NodeFormat_Position>

    func GetPrefixDotPoses: std.core.Array<std.ast.NodeFormat_Position>

    func GetPackageName: std.core.String

    func GetPackageNamePos: std.ast.NodeFormat_Position

    func GetAccessible: std.ast.NodeFormat_AccessibleKind

    func GetHasMacro: Bool
}

class NodeFormat_ImportSpec <: std.ast.FlatBufferObject {
    init(buf: std.core.Array<UInt8>, offset: UInt32) { /* compiled code */ }

    let BASE: UInt16

    let REEXPORT: UInt16

    let IMPORTPOS: UInt16

    let CONTENT: UInt16

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetReExport: std.ast.NodeFormat_ReExportKind

    func GetImportPos: std.ast.NodeFormat_Position

    func GetContent: std.core.Option<std.ast.NodeFormat_ImportContent>
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

    func GetBase: std.core.Option<std.ast.NodeFormat_NodeBase>

    func GetKind: std.ast.NodeFormat_ImportKind

    func GetPrefixPaths: std.core.Array<std.core.Option<std.core.String>>

    func GetPrefixPoses: std.core.Array<std.ast.NodeFormat_Position>

    func GetPrefixDotPoses: std.core.Array<std.ast.NodeFormat_Position>

    func GetIdentifier: std.core.String

    func GetIdentifierPos: std.ast.NodeFormat_Position

    func GetAsPos: std.ast.NodeFormat_Position

    func GetAsIdentifier: std.core.String

    func GetAsIdentifierPos: std.ast.NodeFormat_Position

    func GetLeftCurlPos: std.ast.NodeFormat_Position

    func GetItems: std.core.Array<std.core.Option<std.ast.NodeFormat_ImportContent>>

    func GetCommaPoses: std.core.Array<std.ast.NodeFormat_Position>

    func GetRightCurlPos: std.ast.NodeFormat_Position
}

func isValidPosition(pos: std.ast.NodeFormat_Position): Bool

func isValidPosition(tk: std.ast.Token): Bool

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

    func addPosition(fileID: UInt32, line: Int32, colum: Int32): std.ast.Token

    func addPosition(position: std.ast.NodeFormat_Position): std.ast.Token

    func addPosition(position: std.ast.Position): std.ast.Token

    operator func ==(r: std.ast.Token): Bool

    operator func !=(r: std.ast.Token): Bool

    operator func +(r: std.ast.Tokens): std.ast.Tokens

    operator func +(r: std.ast.Token): std.ast.Tokens

    func dump: Unit

    func toBytes: std.core.Array<UInt8>

    func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64

    func getSize: Int64
}

func isValidToken(tk: std.ast.Token): Bool

func getNodeBeginPos(nodeBase: std.core.Option<std.ast.NodeFormat_NodeBase>): std.ast.NodeFormat_Position

func getOperaterKindOrIdentTokens(opKind: Int32, pos: std.ast.NodeFormat_Position, ident: std.ast.Tokens): std.ast.Tokens

class Tokens <: std.core.Object & std.core.ToString & std.core.Iterable<std.ast.Token> & std.ast.ToBytes {
    var tokens: std.collection.ArrayList<std.ast.Token>

    func refreshPosition: std.ast.Tokens

    init() { /* compiled code */ }

    init(buf: std.core.Array<UInt8>) { /* compiled code */ }

    init(tokArray: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(tokArrayList: std.collection.ArrayList<std.ast.Token>) { /* compiled code */ }

    prop size: Int64

    func `get`(index: Int64): std.ast.Token

    func iterator: std.ast.TokensIterator

    func concat(tokens: std.ast.Tokens): std.ast.Tokens

    operator func [](index: Int64): std.ast.Token

    operator func [](range: std.core.Range<Int64>): std.ast.Tokens

    operator func +(r: std.ast.Tokens): std.ast.Tokens

    operator func +(r: std.ast.Token): std.ast.Tokens

    func append(tokens: std.ast.Tokens): std.ast.Tokens

    func append(token: std.ast.Token): std.ast.Tokens

    func append(node: std.ast.Node): std.ast.Tokens

    func remove(index: Int64): std.ast.Tokens

    func dump: Unit

    func toString: std.core.String

    func toBytes: std.core.Array<UInt8>

    func toBytes(startFrom: Int64): std.core.Array<UInt8>

    func getSize(startFrom: Int64): Int64
}

func getIndent(indent: UInt16): std.core.String

func getTokenIndent(name: std.core.String, tk: std.ast.Token, indent: UInt16, noNameIndent: Bool, index: std.core.Option<Int64>): std.core.String

func dumpNodes(name: std.core.String, contents: std.collection.ArrayList<N>, indent: UInt16): std.core.String

class Node <: std.core.Object & std.ast.ToTokens {
    init() { /* compiled code */ }

    var begin_: std.ast.Position

    var end_: std.ast.Position

    var astKind: std.core.String

    var currentIndent: UInt16

    var beginNode: Bool

    prop beginPos: std.ast.Position

    prop endPos: std.ast.Position

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump: Unit

    func dump(indent: UInt16): std.core.String
}

func compareTokens(tokens1: std.ast.Tokens, tokens2: std.ast.Tokens): Bool

func checkKindValue(k: std.ast.TokenKind, v: std.core.String): Bool

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

    operator func ==(right: std.ast.TokenKind): Bool

    operator func !=(right: std.ast.TokenKind): Bool

    func toString: std.core.String

    func opPrecedence: Int64

    func isNonAssociative: Bool
}

func getImportSpecTokens(imports: std.collection.ArrayList<std.ast.ImportList>): std.ast.Tokens

class ImportList <: std.ast.Node {
    init(reExportToken: std.ast.Token, imp: std.ast.Token, content: std.ast.ImportContent) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var reExportToken_: std.ast.Token

    var import_: std.ast.Token

    var content_: std.ast.ImportContent

    let innerToken_: std.ast.Tokens

    prop modifier: std.ast.Token

    prop keywordI: std.ast.Token

    prop content: std.ast.ImportContent

    func isImportMulti: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func dumpCommonContent(currentIndent: UInt16, upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, body: std.ast.Body): std.core.String

class TypeNode <: std.ast.Node {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var typeParameterName_: std.ast.Token

    var colon_: std.ast.Token

    prop typeParameterName: std.ast.Token

    prop colon: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class GenericConstraint <: std.ast.Node {
    init(keyword: std.ast.Token, ty: std.ast.TypeNode, upbound: std.ast.Token, upperBounds: std.collection.ArrayList<std.ast.TypeNode>, bitAnds: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var ty_: std.ast.TypeNode

    var upbound_: std.ast.Token

    var upperBounds_: std.collection.ArrayList<std.ast.TypeNode>

    var bitAnds_: std.ast.Tokens

    prop keyword: std.ast.Token

    prop typeArgument: std.ast.TypeNode

    prop upperBound: std.ast.Token

    prop upperBounds: std.collection.ArrayList<std.ast.TypeNode>

    prop bitAnds: std.ast.Tokens

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Body <: std.ast.Node {
    init(lBrace: std.ast.Token, decls: std.collection.ArrayList<std.ast.Decl>, rBrace: std.ast.Token) { /* compiled code */ }

    init(decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init() { /* compiled code */ }

    var lBrace_: std.ast.Token

    var decls_: std.collection.ArrayList<std.ast.Decl>

    var rBrace_: std.ast.Token

    prop lBrace: std.ast.Token

    prop decls: std.collection.ArrayList<std.ast.Decl>

    prop rBrace: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func referenceType2Tokens(upperBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, constraint: std.collection.ArrayList<std.ast.GenericConstraint>, constraintCommas: std.ast.Tokens): std.ast.Tokens

extend Tokens {
    func tryGet(index: Int64): std.core.Option<std.ast.Token>
}

extend Token {
    func tryGet: std.core.Option<std.ast.Token>
}

func checkTokenType(tk: std.ast.Token, kind: std.ast.TokenKind): Unit

func checkTokensType(tks: std.ast.Tokens, kind: std.ast.TokenKind): Unit

func checkTokenTypeAssign(tk: std.ast.Token): Unit

func checkPrimitiveType(tk: std.ast.Token): Unit

func checkValid(input: std.ast.Token): std.ast.Token

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

    prop identifier: std.ast.Token

    prop keyword: std.ast.Token

    prop annotations: std.collection.ArrayList<std.ast.Annotation>

    prop modifiers: std.collection.ArrayList<std.ast.Modifier>

    prop genericParam: std.ast.GenericParam

    prop genericConstraint: std.collection.ArrayList<std.ast.GenericConstraint>

    prop constraintCommas: std.ast.Tokens

    func hasAttr(attr: std.core.String): Bool

    func getAttrs: std.ast.Tokens

    prop isGenericDecl: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop at: std.ast.Token

    prop identifier: std.ast.Token

    prop arguments: std.collection.ArrayList<std.ast.Argument>

    prop attributes: std.ast.Tokens

    prop condition: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Modifier <: std.ast.Node {
    init(keyword: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var kind_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class GenericParam <: std.ast.Node {
    init(lAngle: std.ast.Token, parameters: std.ast.Tokens, rAngle: std.ast.Token, commas: std.ast.Tokens) { /* compiled code */ }

    init(parameters: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lAngle_: std.ast.Token

    var parameters_: std.ast.Tokens

    var rAngle_: std.ast.Token

    var commas_: std.ast.Tokens

    prop lAngle: std.ast.Token

    prop parameters: std.ast.Tokens

    prop rAngle: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Visitor {
    var breakTraverseFlag: Bool

    func needBreakTraverse: Bool

    func breakTraverse: Unit

    func visit(_: std.ast.Node): Unit

    func visit(_: std.ast.Expr): Unit

    func visit(_: std.ast.IsExpr): Unit

    func visit(_: std.ast.AsExpr): Unit

    func visit(_: std.ast.LitConstExpr): Unit

    func visit(_: std.ast.UnaryExpr): Unit

    func visit(_: std.ast.BinaryExpr): Unit

    func visit(_: std.ast.ParenExpr): Unit

    func visit(_: std.ast.CallExpr): Unit

    func visit(_: std.ast.RefExpr): Unit

    func visit(_: std.ast.ReturnExpr): Unit

    func visit(_: std.ast.AssignExpr): Unit

    func visit(_: std.ast.MemberAccess): Unit

    func visit(_: std.ast.IfExpr): Unit

    func visit(_: std.ast.LetPatternExpr): Unit

    func visit(_: std.ast.MatchExpr): Unit

    func visit(_: std.ast.WhileExpr): Unit

    func visit(_: std.ast.LambdaExpr): Unit

    func visit(_: std.ast.SpawnExpr): Unit

    func visit(_: std.ast.SynchronizedExpr): Unit

    func visit(_: std.ast.TrailingClosureExpr): Unit

    func visit(_: std.ast.TypeConvExpr): Unit

    func visit(_: std.ast.ForInExpr): Unit

    func visit(_: std.ast.PrimitiveTypeExpr): Unit

    func visit(_: std.ast.ArrayLiteral): Unit

    func visit(_: std.ast.TupleLiteral): Unit

    func visit(_: std.ast.SubscriptExpr): Unit

    func visit(_: std.ast.RangeExpr): Unit

    func visit(_: std.ast.Block): Unit

    func visit(_: std.ast.DoWhileExpr): Unit

    func visit(_: std.ast.JumpExpr): Unit

    func visit(_: std.ast.IncOrDecExpr): Unit

    func visit(_: std.ast.TryExpr): Unit

    func visit(_: std.ast.ThrowExpr): Unit

    func visit(_: std.ast.OptionalExpr): Unit

    func visit(_: std.ast.QuoteExpr): Unit

    func visit(_: std.ast.MacroExpandExpr): Unit

    func visit(_: std.ast.WildcardExpr): Unit

    func visit(_: std.ast.VArrayExpr): Unit

    func visit(_: std.ast.Decl): Unit

    func visit(_: std.ast.ClassDecl): Unit

    func visit(_: std.ast.FuncDecl): Unit

    func visit(_: std.ast.MainDecl): Unit

    func visit(_: std.ast.MacroDecl): Unit

    func visit(_: std.ast.StructDecl): Unit

    func visit(_: std.ast.InterfaceDecl): Unit

    func visit(_: std.ast.PropDecl): Unit

    func visit(_: std.ast.VarDecl): Unit

    func visit(_: std.ast.PrimaryCtorDecl): Unit

    func visit(_: std.ast.EnumDecl): Unit

    func visit(_: std.ast.FuncParam): Unit

    func visit(_: std.ast.TypeAliasDecl): Unit

    func visit(_: std.ast.ExtendDecl): Unit

    func visit(_: std.ast.MacroExpandDecl): Unit

    func visit(_: std.ast.TypeNode): Unit

    func visit(_: std.ast.RefType): Unit

    func visit(_: std.ast.PrimitiveType): Unit

    func visit(_: std.ast.FuncType): Unit

    func visit(_: std.ast.ThisType): Unit

    func visit(_: std.ast.ParenType): Unit

    func visit(_: std.ast.QualifiedType): Unit

    func visit(_: std.ast.PrefixType): Unit

    func visit(_: std.ast.TupleType): Unit

    func visit(_: std.ast.VArrayType): Unit

    func visit(_: std.ast.Pattern): Unit

    func visit(_: std.ast.ConstPattern): Unit

    func visit(_: std.ast.WildcardPattern): Unit

    func visit(_: std.ast.VarPattern): Unit

    func visit(_: std.ast.VarOrEnumPattern): Unit

    func visit(_: std.ast.ExceptTypePattern): Unit

    func visit(_: std.ast.TypePattern): Unit

    func visit(_: std.ast.EnumPattern): Unit

    func visit(_: std.ast.TuplePattern): Unit

    func visit(_: std.ast.GenericParam): Unit

    func visit(_: std.ast.GenericConstraint): Unit

    func visit(_: std.ast.MatchCase): Unit

    func visit(_: std.ast.Program): Unit

    func visit(_: std.ast.PackageHeader): Unit

    func visit(_: std.ast.ImportList): Unit

    func visit(_: std.ast.ImportContent): Unit

    func visit(_: std.ast.Constructor): Unit

    func visit(_: std.ast.Argument): Unit

    func visit(_: std.ast.Annotation): Unit

    func visit(_: std.ast.Modifier): Unit

    func visit(_: std.ast.Body): Unit

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

    prop upperBound: std.ast.Token

    prop superTypes: std.collection.ArrayList<std.ast.TypeNode>

    prop superTypeBitAnds: std.ast.Tokens

    prop body: std.ast.Body

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class StructDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var structbody_: std.ast.Body

    prop upperBound: std.ast.Token

    prop superTypes: std.collection.ArrayList<std.ast.TypeNode>

    prop superTypeBitAnds: std.ast.Tokens

    prop body: std.ast.Body

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class InterfaceDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, upBound: std.ast.Token, superTypes: std.collection.ArrayList<std.ast.TypeNode>, superTypeBitAnds: std.ast.Tokens, body: std.ast.Body) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var upBound_: std.ast.Token

    var superTypes_: std.collection.ArrayList<std.ast.TypeNode>

    var superTypeBitAnds_: std.ast.Tokens

    var interfacetbody_: std.ast.Body

    prop upperBound: std.ast.Token

    prop superTypes: std.collection.ArrayList<std.ast.TypeNode>

    prop superTypeBitAnds: std.ast.Tokens

    prop body: std.ast.Body

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop upperBound: std.ast.Token

    prop superTypes: std.collection.ArrayList<std.ast.TypeNode>

    prop superTypeBitAnds: std.ast.Tokens

    prop lBrace: std.ast.Token

    prop constructors: std.collection.ArrayList<std.ast.Constructor>

    prop decls: std.collection.ArrayList<std.ast.Decl>

    prop rBrace: std.ast.Token

    prop ellipsis: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop annotations: std.collection.ArrayList<std.ast.Annotation>

    prop identifier: std.ast.Token

    prop lParen: std.ast.Token

    prop typeArguments: std.collection.ArrayList<std.ast.TypeNode>

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop identifier: std.ast.Token

    prop extendType: std.ast.TypeNode

    prop upperBound: std.ast.Token

    prop superTypes: std.collection.ArrayList<std.ast.TypeNode>

    prop superTypeBitAnds: std.ast.Tokens

    prop body: std.ast.Body

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop overloadOp: std.ast.Tokens

    prop lParen: std.ast.Token

    prop funcParams: std.collection.ArrayList<std.ast.FuncParam>

    prop rParen: std.ast.Token

    prop colon: std.ast.Token

    prop declType: std.ast.TypeNode

    prop block: std.ast.Block

    func isConst: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop not: std.ast.Token

    prop colon: std.ast.Token

    prop paramType: std.ast.TypeNode

    prop assign: std.ast.Token

    prop expr: std.ast.Expr

    func isMemberParam: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Block <: std.ast.Expr {
    init(lCurl: std.ast.Token, nodes: std.collection.ArrayList<std.ast.Node>, rCurl: std.ast.Token, unsafe_: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var lCurl_: std.ast.Token

    var nodes_: std.collection.ArrayList<std.ast.Node>

    var rCurl_: std.ast.Token

    var unsafe_: std.ast.Token

    prop lBrace: std.ast.Token

    prop nodes: std.collection.ArrayList<std.ast.Node>

    prop rBrace: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop lParen: std.ast.Token

    prop funcParams: std.collection.ArrayList<std.ast.FuncParam>

    prop rParen: std.ast.Token

    prop colon: std.ast.Token

    prop declType: std.ast.TypeNode

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop lParen: std.ast.Token

    prop funcParams: std.collection.ArrayList<std.ast.FuncParam>

    prop rParen: std.ast.Token

    prop colon: std.ast.Token

    prop declType: std.ast.TypeNode

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class PrimaryCtorDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, lParen: std.ast.Token, params: std.collection.ArrayList<std.ast.FuncParam>, rParen: std.ast.Token, block: std.ast.Block) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var rParen_: std.ast.Token

    var params_: std.collection.ArrayList<std.ast.FuncParam>

    var block_: std.ast.Block

    prop lParen: std.ast.Token

    prop funcParams: std.collection.ArrayList<std.ast.FuncParam>

    prop rParen: std.ast.Token

    prop block: std.ast.Block

    func isConst: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop pattern: std.ast.Pattern

    prop colon: std.ast.Token

    prop declType: std.ast.TypeNode

    prop assign: std.ast.Token

    prop expr: std.ast.Expr

    func isConst: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Expr <: std.ast.Node {
    init() { /* compiled code */ }

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(_: UInt16): std.core.String

    func precedence: Int64
}

class Pattern <: std.ast.Node {
    init() { /* compiled code */ }

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(_: UInt16): std.core.String
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

    prop colon: std.ast.Token

    prop declType: std.ast.TypeNode

    prop lBrace: std.ast.Token

    prop getter: std.ast.FuncDecl

    prop setter: std.ast.FuncDecl

    prop rBrace: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class TypeAliasDecl <: std.ast.Decl {
    init(decl: std.ast.Decl, assign: std.ast.Token, aliasType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var assign_: std.ast.Token

    var aliasType_: std.ast.TypeNode

    prop assign: std.ast.Token

    prop aliasType: std.ast.TypeNode

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop fullIdentifier: std.ast.Token

    prop lSquare: std.ast.Token

    prop macroAttrs: std.ast.Tokens

    prop rSquare: std.ast.Token

    prop lParen: std.ast.Token

    prop macroInputs: std.ast.Tokens

    prop rParen: std.ast.Token

    prop macroInputDecl: std.ast.Decl

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop fullIdentifier: std.ast.Token

    prop lSquare: std.ast.Token

    prop macroAttrs: std.ast.Tokens

    prop rSquare: std.ast.Token

    prop lParen: std.ast.Token

    prop macroInputs: std.ast.Tokens

    prop rParen: std.ast.Token

    prop macroInputDecl: std.ast.Decl

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func boundsCheck(buf: std.core.Array<UInt8>, sz: Int64): Bool

func getUInt8(buf: std.core.Array<UInt8>): UInt8

func getBool(buf: std.core.Array<UInt8>): Bool

func getUInt16(buf: std.core.Array<UInt8>): UInt16

func getUInt32(buf: std.core.Array<UInt8>): UInt32

func getInt32(buf: std.core.Array<UInt8>): Int32

func getUInt64(buf: std.core.Array<UInt8>): UInt64

func getString(buf: std.core.Array<UInt8>, offset: Int64, len: UInt32): std.core.String

enum DiagReportLevel {
    | ERROR

    | WARNING

    func level: Int32
}

func diagReport(level: std.ast.DiagReportLevel, tokens: std.ast.Tokens, message: std.core.String, hint: std.core.String): Unit

class ASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class ParseASTException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class BinaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(leftExpr: std.ast.Expr, op: std.ast.Token, rightExpr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    var lExpr_: std.ast.Expr

    var rExpr_: std.ast.Expr

    var op_: std.ast.Token

    prop leftExpr: std.ast.Expr

    prop op: std.ast.Token

    prop rightExpr: std.ast.Expr

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class UnaryExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(op: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init() { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    prop op: std.ast.Token

    prop expr: std.ast.Expr

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class IsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, shiftType: std.ast.TypeNode) { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    var shiftType_: std.ast.TypeNode

    prop expr: std.ast.Expr

    prop keyword: std.ast.Token

    prop shiftType: std.ast.TypeNode

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class AsExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, op: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    var expr_: std.ast.Expr

    var op_: std.ast.Token

    var shiftType_: std.ast.TypeNode

    prop expr: std.ast.Expr

    prop keyword: std.ast.Token

    prop shiftType: std.ast.TypeNode

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ParenExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    var lParen_: std.ast.Token

    var expr_: std.ast.Expr

    var rParen_: std.ast.Token

    prop lParen: std.ast.Token

    prop parenthesizedExpr: std.ast.Expr

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class LitConstExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(literal: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var literal_: std.ast.Token

    prop literal: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop identifier: std.ast.Token

    prop lAngle: std.ast.Token

    prop typeArguments: std.collection.ArrayList<std.ast.TypeNode>

    prop commas: std.ast.Tokens

    prop rAngle: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ReturnExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.core.Option<std.ast.Expr>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    var expr_: std.core.Option<std.ast.Expr>

    prop keyword: std.ast.Token

    prop expr: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ThrowExpr <: std.ast.Expr {
    init(keyWord: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    var expr_: std.ast.Expr

    prop keyword: std.ast.Token

    prop expr: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class AssignExpr <: std.ast.Expr {
    init(lexpr: std.ast.Expr, assign: std.ast.Token, rexpr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var assign_: std.ast.Token

    var lexpr_: std.ast.Expr

    var rexpr_: std.ast.Expr

    prop assign: std.ast.Token

    prop leftExpr: std.ast.Expr

    prop rightExpr: std.ast.Expr

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class CallExpr <: std.ast.Expr {
    init(callSuffix: std.ast.Expr, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var postfixExpr_: std.ast.Expr

    var lParen_: std.ast.Token

    var args_: std.collection.ArrayList<std.ast.Argument>

    var rParen_: std.ast.Token

    prop callFunc: std.ast.Expr

    prop lParen: std.ast.Token

    prop arguments: std.collection.ArrayList<std.ast.Argument>

    prop rParen: std.ast.Token

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class Argument <: std.ast.Node {
    init(inout_: std.ast.Token, identifier: std.ast.Token, colon: std.ast.Token, expr: std.ast.Expr, comma: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var inout_: std.ast.Token

    var identifier_: std.ast.Token

    var colon_: std.ast.Token

    var expr_: std.ast.Expr

    var comma_: std.ast.Token

    prop keyword: std.ast.Token

    prop identifier: std.ast.Token

    prop colon: std.ast.Token

    prop expr: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop baseExpr: std.ast.Expr

    prop dot: std.ast.Token

    prop field: std.ast.Token

    prop lAngle: std.ast.Token

    prop typeArguments: std.collection.ArrayList<std.ast.TypeNode>

    prop commas: std.ast.Tokens

    prop rAngle: std.ast.Token

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keywordI: std.ast.Token

    prop lParen: std.ast.Token

    prop condition: std.ast.Expr

    prop rParen: std.ast.Token

    prop ifBlock: std.ast.Block

    prop keywordE: std.ast.Token

    prop elseExpr: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class LetPatternExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(key: std.ast.Token, pattern: std.ast.Pattern, backArrow: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    var key_: std.ast.Token

    var pattern_: std.ast.Pattern

    var backArrow_: std.ast.Token

    var expr_: std.ast.Expr

    prop keyword: std.ast.Token

    prop pattern: std.ast.Pattern

    prop backArrow: std.ast.Token

    prop expr: std.ast.Expr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop selector: std.ast.Expr

    prop rParen: std.ast.Token

    prop lBrace: std.ast.Token

    prop matchCases: std.collection.ArrayList<std.ast.MatchCase>

    prop rBrace: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keywordC: std.ast.Token

    prop expr: std.ast.Expr

    prop patterns: std.collection.ArrayList<std.ast.Pattern>

    prop bitOrs: std.ast.Tokens

    prop keywordW: std.ast.Token

    prop patternGuard: std.ast.Expr

    prop arrow: std.ast.Token

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop condition: std.ast.Expr

    prop rParen: std.ast.Token

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keywordD: std.ast.Token

    prop block: std.ast.Block

    prop keywordW: std.ast.Token

    prop lParen: std.ast.Token

    prop condition: std.ast.Expr

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop lBrace: std.ast.Token

    prop funcParams: std.collection.ArrayList<std.ast.FuncParam>

    prop doubleArrow: std.ast.Token

    prop nodes: std.collection.ArrayList<std.ast.Node>

    prop rBrace: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop threadContext: std.ast.Expr

    prop rParen: std.ast.Token

    prop lambdaExpr: std.ast.LambdaExpr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop structuredMutex: std.ast.Expr

    prop rParen: std.ast.Token

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class TrailingClosureExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(expr: std.ast.Expr, lambda: std.ast.LambdaExpr) { /* compiled code */ }

    var expr_: std.ast.Expr

    var lambda_: std.ast.LambdaExpr

    prop expr: std.ast.Expr

    prop lambdaExpr: std.ast.LambdaExpr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class TypeConvExpr <: std.ast.Expr {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(ty: std.ast.PrimitiveType, lParen: std.ast.Token, expr: std.ast.Expr, rParen: std.ast.Token) { /* compiled code */ }

    var type_: std.ast.PrimitiveType

    var lParen_: std.ast.Token

    var expr_: std.ast.Expr

    var rParen_: std.ast.Token

    prop targetType: std.ast.PrimitiveType

    prop lParen: std.ast.Token

    prop expr: std.ast.Expr

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class PrimitiveType <: std.ast.TypeNode {
    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    var keyword_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keywordF: std.ast.Token

    prop lParen: std.ast.Token

    prop pattern: std.ast.Pattern

    prop keywordI: std.ast.Token

    prop expr: std.ast.Expr

    prop keywordW: std.ast.Token

    prop patternGuard: std.ast.Expr

    prop rParen: std.ast.Token

    prop block: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class PrimitiveTypeExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(kind: std.ast.Token) { /* compiled code */ }

    var keyword_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ArrayLiteral <: std.ast.Expr {
    init(lSquare: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lSquare_: std.ast.Token

    var elements_: std.collection.ArrayList<std.ast.Expr>

    var commas_: std.ast.Tokens

    var rSquare_: std.ast.Token

    prop lSquare: std.ast.Token

    prop elements: std.collection.ArrayList<std.ast.Expr>

    prop rSquare: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class TupleLiteral <: std.ast.Expr {
    init(lParen: std.ast.Token, elements: std.collection.ArrayList<std.ast.Expr>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var elements_: std.collection.ArrayList<std.ast.Expr>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    prop lParen: std.ast.Token

    prop elements: std.collection.ArrayList<std.ast.Expr>

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop start: std.ast.Expr

    prop op: std.ast.Token

    prop end: std.ast.Expr

    prop colon: std.ast.Token

    prop step: std.ast.Expr

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class SubscriptExpr <: std.ast.Expr {
    init(postfixExpr: std.ast.Expr, lSquare: std.ast.Token, index: std.collection.ArrayList<std.ast.Expr>, rSquare: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var postfixExpr_: std.ast.Expr

    var lSquare_: std.ast.Token

    var index_: std.collection.ArrayList<std.ast.Expr>

    var rSquare_: std.ast.Token

    prop baseExpr: std.ast.Expr

    prop lSquare: std.ast.Token

    prop indexList: std.collection.ArrayList<std.ast.Expr>

    prop rSquare: std.ast.Token

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class JumpExpr <: std.ast.Expr {
    init(kind: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var kind_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class IncOrDecExpr <: std.ast.Expr {
    init(kind: std.ast.Token, expr: std.ast.Expr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var op_: std.ast.Token

    var expr_: std.ast.Expr

    prop op: std.ast.Token

    prop expr: std.ast.Expr

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keywordT: std.ast.Token

    prop resourceSpec: std.collection.ArrayList<std.ast.VarDecl>

    prop tryBlock: std.ast.Block

    prop keywordsC: std.ast.Tokens

    prop catchPatterns: std.collection.ArrayList<std.ast.Pattern>

    prop catchBlocks: std.collection.ArrayList<std.ast.Block>

    prop keywordF: std.ast.Token

    prop finallyBlock: std.ast.Block

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class OptionalExpr <: std.ast.Expr {
    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init(baseExpr: std.ast.Expr, quest: std.ast.Token) { /* compiled code */ }

    var baseExpr_: std.ast.Expr

    var quest_: std.ast.Token

    prop baseExpr: std.ast.Expr

    prop quest: std.ast.Token

    func precedence: Int64

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class QuoteToken <: std.ast.Expr {
    init(tokens: std.ast.Tokens) { /* compiled code */ }

    var tokens_: std.ast.Tokens

    prop tokens: std.ast.Tokens

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class QuoteExpr <: std.ast.Expr {
    init(keyword: std.ast.Token, lParen: std.ast.Token, exprs: std.collection.ArrayList<std.ast.Expr>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    var lParen_: std.ast.Token

    var exprs_: std.collection.ArrayList<std.ast.Expr>

    var rParen_: std.ast.Token

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop exprs: std.collection.ArrayList<std.ast.Expr>

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class WildcardExpr <: std.ast.Expr {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class VArrayExpr <: std.ast.Expr {
    init(ty: std.ast.VArrayType, lParen: std.ast.Token, args: std.collection.ArrayList<std.ast.Argument>, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var ty_: std.ast.VArrayType

    var lParen_: std.ast.Token

    var args_: std.collection.ArrayList<std.ast.Argument>

    var rParen_: std.ast.Token

    prop vArrayType: std.ast.VArrayType

    prop lParen: std.ast.Token

    prop arguments: std.collection.ArrayList<std.ast.Argument>

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lAngle: std.ast.Token

    prop elementTy: std.ast.TypeNode

    prop dollar: std.ast.Token

    prop size: std.ast.Token

    prop rAngle: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop at: std.ast.Token

    prop identifier: std.ast.Token

    prop lSquare: std.ast.Token

    prop macroAttrs: std.ast.Tokens

    prop rSquare: std.ast.Token

    prop lParen: std.ast.Token

    prop macroInputs: std.ast.Tokens

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func addParen(ret: std.ast.Tokens, innerExpr: std.ast.Expr, thisExpr: std.ast.Expr, addOnEqualPred: Bool): std.ast.Tokens

let MACRO_OBJECT: std.core.ThreadLocal<CPointer<Unit>>

func CJ_AST_ParseExpr(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

struct ParseRes {
    let node: CPointer<UInt8>

    let errMsg: CString

    init() { /* compiled code */ }
}

func CJ_AST_ParseDecl(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

func CJ_AST_ParsePropMemberDecl(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

func CJ_AST_ParsePrimaryConstructor(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

func CJ_AST_ParsePattern(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

func CJ_AST_ParseType(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>, tokenCounter: CPointer<Int64>): CPointer<std.ast.ParseRes>

func CJ_AST_ParseTopLevel(fptr: CPointer<Unit>, tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

func CJ_AST_ParseAnnotationArguments(tokensBytes: CPointer<UInt8>): CPointer<std.ast.ParseRes>

func CJ_AST_Lex(fptr: CPointer<Unit>, code: CString): CPointer<UInt8>

func CJ_CheckParentContext(fptr: CPointer<Unit>, parent: CString, report: Bool): Bool

func CJ_SetItemInfo(fptr: CPointer<Unit>, key: CString, value: CPointer<Unit>, valueType: UInt8): Unit

func CJ_GetChildMessages(fptr: CPointer<Unit>, children: CString): CPointer<CPointer<CPointer<Unit>>>

func CJ_CheckAddSpace(tokBytes: CPointer<UInt8>, spaceFlag: CPointer<Bool>): Unit

func CJ_AST_DiagReport(fptr: CPointer<Unit>, level: CPointer<Int32>, tokensBytes: CPointer<UInt8>, message: CString, hint: CString): UInt8

func CJ_GetMacroPosition(fptr: CPointer<Unit>, fileID: CPointer<UInt32>, line: CPointer<Int32>, column: CPointer<Int32>): Unit

func CJ_AST_Float64ToCPointer(num: Float64): CString

let BUFFER_HEADER_SIZE: Int64

func getInt32FromUnsafePointer(ptr: CPointer<UInt8>): Int32

func parse(input: std.ast.Tokens, nativeParse: (CPointer<UInt8>) -> CPointer<std.ast.ParseRes>): std.ast.NodeFormat_Node

func parseFragment(input: std.ast.Tokens, startFrom: Int64, nativeParse: (CPointer<UInt8>, CPointer<Int64>) -> CPointer<std.ast.ParseRes>): (std.ast.NodeFormat_Node, Int64)

func genNodeFormatNode(pNode: CPointer<UInt8>, pTokensBytes: CPointer<UInt8>): std.ast.NodeFormat_Node

func getTokensFromBytes(pTokenArray: CPointer<UInt8>): std.ast.Tokens

func cangjieLex(code: std.core.String): std.ast.Tokens

func cangjieLex(code: std.core.String, truncated: Bool): std.ast.Tokens

func unsafePointerCastFromUint8Array(arr: std.core.Array<UInt8>): CPointer<UInt8>

class MacroContextException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func assertParentContext(parentMacroName: std.core.String): Unit

func insideParentContext(parentMacroName: std.core.String): Bool

let ITEM_TYPE_STRING: UInt8

let ITEM_TYPE_INT64: UInt8

let ITEM_TYPE_BOOL: UInt8

let ITEM_TYPE_TOKENS: UInt8

func setItem(key: std.core.String, value: std.core.String): Unit

func setItem(key: std.core.String, value: Int64): Unit

func setItem(key: std.core.String, value: Bool): Unit

func setItem(key: std.core.String, value: std.ast.Tokens): Unit

class McInfo {
    var keyVal: CString

    var value_: CPointer<Unit>

    var valueType_: UInt8

    init(keyVal_: CString, v: CPointer<Unit>, vType: UInt8) { /* compiled code */ }

    prop keyV: CString

    prop value: CPointer<Unit>

    prop valueType: UInt8
}

class MacroMessage {
    var infoMap: std.collection.ArrayList<std.ast.McInfo>

    init(res: std.collection.ArrayList<std.ast.McInfo>) { /* compiled code */ }

    func hasItem(key: std.core.String): Bool

    func getString(key: std.core.String): std.core.String

    func getInt64(key: std.core.String): Int64

    func getBool(key: std.core.String): Bool

    func getTokens(key: std.core.String): std.ast.Tokens
}

func readCpointerToMap(cp: CPointer<CPointer<Unit>>): std.collection.ArrayList<std.ast.McInfo>

func getChildMessages(children: std.core.String): std.collection.ArrayList<std.ast.MacroMessage>

class Table {
    var bytes: std.core.Array<UInt8>

    var pos: UInt32

    init(buf: std.core.Array<UInt8>, position: UInt32) { /* compiled code */ }

    func offset(slot: UInt16): UInt16

    func getIndirect(offsetVal: UInt32): UInt32

    func getByteVector(offsetVal: UInt32): std.core.Array<UInt8>

    func getVectorLenBySlot(slot: UInt16): Int64

    func getVectorStartBySlot(slot: UInt16): UInt32

    func getString(offsetVal: UInt32): std.core.String

    func getUInt8(offsetVal: UInt32): UInt8

    func getInt8(offsetVal: UInt32): Int8

    func getBool(offsetVal: UInt32): Bool

    func getUInt16(offsetVal: UInt32): UInt16

    func getInt16(offsetVal: UInt32): Int16

    func getUInt32(offsetVal: UInt32): UInt32

    func getInt32(offsetVal: UInt32): Int32

    func getUInt64(offsetVal: UInt32): UInt64

    func getInt64(offsetVal: UInt32): Int64

    func getUInt8Slot(slot: UInt16, default: UInt8): UInt8

    func getInt8Slot(slot: UInt16, default: Int8): Int8

    func getBoolSlot(slot: UInt16, default: Bool): Bool

    func getUInt16Slot(slot: UInt16, default: UInt16): UInt16

    func getInt16Slot(slot: UInt16, default: Int16): Int16

    func getUInt32Slot(slot: UInt16, default: UInt32): UInt32

    func getInt32Slot(slot: UInt16, default: Int32): Int32

    func getUInt64Slot(slot: UInt16, default: UInt64): UInt64

    func getInt64Slot(slot: UInt16, default: Int64): Int64
}

struct Position <: std.ast.ToBytes {
    let fileID: UInt32

    let line: Int32

    let column: Int32

    init() { /* compiled code */ }

    init(fileID: UInt32, line: Int32, column: Int32) { /* compiled code */ }

    func isEmpty: Bool

    operator func ==(r: std.ast.Position): Bool

    operator func !=(r: std.ast.Position): Bool

    func dump: Unit

    func toBytes: std.core.Array<UInt8>

    func toBytes(arr: std.core.Array<UInt8>, start1: Int64): Int64

    func getSize: Int64
}

interface ToTokens {
    func toTokens: std.ast.Tokens
}

class Program <: std.ast.Node {
    init(pkgHeader: std.ast.PackageHeader, importLists: std.collection.ArrayList<std.ast.ImportList>, decls: std.collection.ArrayList<std.ast.Decl>) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var pkgHeader_: std.ast.PackageHeader

    var importLists_: std.collection.ArrayList<std.ast.ImportList>

    var decls_: std.collection.ArrayList<std.ast.Decl>

    prop packageHeader: std.ast.PackageHeader

    prop importLists: std.collection.ArrayList<std.ast.ImportList>

    prop decls: std.collection.ArrayList<std.ast.Decl>

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop accessible: std.ast.Token

    prop keywordM: std.ast.Token

    prop keywordP: std.ast.Token

    prop prefixPaths: std.ast.Tokens

    prop prefixDots: std.ast.Tokens

    prop packageIdentifier: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String

    func addPackageTokens(input: std.ast.Tokens): std.ast.Tokens
}

enum ImportKind <: std.core.ToString {
    | Single

    | Alias

    | All

    | Multi

    func toString: std.core.String
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

    prop importKind: std.ast.ImportKind

    prop prefixPaths: std.ast.Tokens

    prop prefixDots: std.ast.Tokens

    prop identifier: std.ast.Token

    prop importAlias: std.ast.Tokens

    prop lBrace: std.ast.Token

    prop items: std.collection.ArrayList<std.ast.ImportContent>

    prop commas: std.ast.Tokens

    prop rBrace: std.ast.Token

    func isImportAlias: Bool

    func isImportAll: Bool

    func isImportMulti: Bool

    func isImportSingle: Bool

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

extend ArrayList <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Array <: ToTokens {
    func toTokens: std.ast.Tokens
}

func parseDecl(input: std.ast.Tokens, astKind: std.core.String): std.ast.Decl

func parseDeclFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Decl, Int64)

func generateDeclAST(declNode: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Decl

func createDeclBase(keyWord: std.ast.Token, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl

func generateGenericInfo(generic: std.core.Option<std.ast.NodeFormat_Generic>): (std.core.Option<std.ast.GenericParam>, std.collection.ArrayList<std.ast.GenericConstraint>, std.ast.Tokens, Bool)

func createClassDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ClassDecl

func createStrucDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.StructDecl

func createInterfaceDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.InterfaceDecl

func createEnumDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.EnumDecl

func createExtendDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.ExtendDecl

func createFuncDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.Decl

func createFuncParam(decl: std.ast.NodeFormat_Decl): std.ast.Decl

func createFuncParam(param: std.core.Option<std.ast.NodeFormat_FuncParam>, index: Int64, length: Int64): std.ast.FuncParam

func generateFuncParams(funcBody: std.ast.NodeFormat_FuncBody): std.collection.ArrayList<std.ast.FuncParam>

func generateCommonFuncDecl(funcDecl: std.core.Option<std.ast.NodeFormat_FuncDecl>, declNode: std.ast.Decl): std.ast.FuncDecl

func createFuncBody(funcBody: std.core.Option<std.ast.NodeFormat_FuncBody>): (std.collection.ArrayList<std.ast.FuncParam>, std.ast.Token, std.core.Option<std.ast.TypeNode>, std.ast.Block, std.ast.Token, std.ast.Token)

func createMainDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MainDecl

func createMacroDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.MacroDecl

func createPrimaryCtorDecl(decl: std.ast.NodeFormat_Decl, declNode: std.ast.Decl): std.ast.PrimaryCtorDecl

func createVarDeclNode(decl: std.core.Option<std.ast.NodeFormat_VarDecl>): std.ast.VarDecl

func createVarDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl

func createVarWithPatternDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl

func createPropDecl(decl: std.ast.NodeFormat_Decl): std.ast.PropDecl

func createTypeAliasDecl(decl: std.ast.NodeFormat_Decl, declBase: std.ast.NodeFormat_DeclBase): std.ast.TypeAliasDecl

func createMacroExpandDecl(decl: std.ast.NodeFormat_Decl): std.ast.Decl

func parseExpr(input: std.ast.Tokens): std.ast.Expr

func parseExprFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Expr, Int64)

func generateExprAST(exprNode: std.core.Option<std.ast.NodeFormat_Expr>): std.ast.Expr

func createBlock(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func generateBlock(blockOption: std.core.Option<std.ast.NodeFormat_Block>): std.ast.Block

func createBinaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createUnaryExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createIsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createAsExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createParenExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createLitConstExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createCallExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createRefExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func generateRefExpr(ref: std.core.Option<std.ast.NodeFormat_RefExpr>): std.ast.RefExpr

func createReturnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createThrowExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createAssignExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createMemberAccess(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createOptionalExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createOptionalChainExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createIfExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createLetPatternExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.LetPatternExpr

func generateMatchCase(mc: std.core.Option<std.ast.NodeFormat_MatchCase>): std.ast.MatchCase

func generateMatchCaseOther(mco: std.core.Option<std.ast.NodeFormat_MatchCaseOther>): std.ast.MatchCase

func createMatchExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createDoWhileExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createLambdaExpr(lambdaExpr: std.ast.NodeFormat_LambdaExpr): std.ast.LambdaExpr

func createLambdaExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createSpawnExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createSynchronizedExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createTrailingClosureExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createTypeConvExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createForInExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createPrimitiveTypeExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createArrayLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createTupleLiteral(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createRangeExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createSubscriptExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createJumpExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createIncOrDecExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createTryExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createQuoteExpr(expr: std.ast.NodeFormat_Expr, exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createQuoteToken(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createWildcardExpr(exprBase: std.ast.NodeFormat_NodeBase): std.ast.Expr

func createArrayExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

func createMacroExpandExpr(expr: std.ast.NodeFormat_Expr): std.ast.Expr

let EXPR_LIST: std.core.Array<std.core.String>

let DECL_LIST: std.core.Array<std.core.String>

let TYPE_LIST: std.core.Array<std.core.String>

let PATTERN_LIST: std.core.Array<std.core.String>

func parseProgram(input: std.ast.Tokens): std.ast.Program

func addPositionInfo(node: std.ast.Node, nodeBase: std.ast.NodeFormat_NodeBase): Unit

func generateNode(n: std.core.Option<std.ast.NodeFormat_Node>): std.ast.Node

func createAnnotation(anno: std.core.Option<std.ast.NodeFormat_Annotation>): std.ast.Annotation

func createModifier(modifier: std.core.Option<std.ast.NodeFormat_Modifier>): std.ast.Modifier

func createArgument(arg: std.core.Option<std.ast.NodeFormat_FuncArg>): std.ast.Argument

func generateGenericParamTk(gp: std.core.Option<std.ast.NodeFormat_GenericParamDecl>): (std.ast.Token, std.ast.Token)

func createGenericParam(gic: std.core.Option<std.ast.NodeFormat_Generic>): std.core.Option<std.ast.GenericParam>

func createGenericConstraint(flag: Bool, cons: std.core.Option<std.ast.NodeFormat_GenericConstraint>): std.ast.GenericConstraint

func createMacroExpand(nodeInvocation: std.core.Option<std.ast.NodeFormat_MacroInvocation>, isDecl: Bool, isFuncParam: Bool): std.ast.Node

func processCustomAnnotationLikeMacro(macroInputDecl: std.core.Option<std.ast.Decl>, macroNode: std.ast.Decl): std.ast.Decl

func getPackageIdentToken(s: std.core.String): std.ast.Token

func getPackagePrefixes(prefixPaths: std.core.Array<std.core.Option<std.core.String>>, prefixPoses: std.core.Array<std.ast.NodeFormat_Position>, prefixDotPoses: std.core.Array<std.ast.NodeFormat_Position>): (std.ast.Tokens, std.ast.Tokens)

func isSamePos(pos1: std.ast.NodeFormat_Position, pos2: std.ast.NodeFormat_Position): Bool

func createPackageHeader(pkgHeader: std.core.Option<std.ast.NodeFormat_PackageSpec>): std.ast.PackageHeader

func createImportContent(content: std.core.Option<std.ast.NodeFormat_ImportContent>): std.ast.ImportContent

func createImportList(imports: std.core.Option<std.ast.NodeFormat_ImportSpec>): std.ast.ImportList

func createProgram(file: std.core.Option<std.ast.NodeFormat_File>): std.ast.Program

func createConstructor(decl: std.core.Option<std.ast.NodeFormat_Decl>): std.ast.Constructor

func createArgumentTokens(m: std.ast.Decl): std.ast.Tokens

func createCustomAnnotation(m: std.ast.Decl): std.ast.Annotation

func createConstructorLikeMacroExpand(decl: std.ast.NodeFormat_Decl): std.ast.Constructor

func createConstructorWithAt(decl: std.ast.Decl): std.ast.Constructor

func createConstructorWithAtExcl(decl: std.ast.Decl): std.ast.Constructor

func parsePattern(input: std.ast.Tokens): std.ast.Pattern

func parsePatternFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.Pattern, Int64)

func generatePatternAST(patternNode: std.core.Option<std.ast.NodeFormat_Pattern>): std.ast.Pattern

func createVarPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.VarPattern

class VarPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    prop identifier: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createConstPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ConstPattern

class ConstPattern <: std.ast.Pattern {
    init(litConstExpr: std.ast.LitConstExpr) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var litConstExpr_: std.ast.LitConstExpr

    prop litConstExpr: std.ast.LitConstExpr

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createWildcardPattern(_: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.WildcardPattern

class WildcardPattern <: std.ast.Pattern {
    init(keyword: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyWord_: std.ast.Token

    prop wildcard: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createVarOrEnumPattern(pattern: std.ast.NodeFormat_Pattern, pos: std.ast.NodeFormat_Position): std.ast.VarOrEnumPattern

class VarOrEnumPattern <: std.ast.Pattern {
    init(identifier: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    var identifier_: std.ast.Token

    prop identifier: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createExceptTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.ExceptTypePattern

class ExceptTypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, bitOr: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var pattern_: std.ast.Pattern

    var colon_: std.ast.Token

    var types_: std.collection.ArrayList<std.ast.TypeNode>

    var bitOr_: std.ast.Tokens

    prop pattern: std.ast.Pattern

    prop colon: std.ast.Token

    prop types: std.collection.ArrayList<std.ast.TypeNode>

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createTypePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TypePattern

class TypePattern <: std.ast.Pattern {
    init(pattern: std.ast.Pattern, colon: std.ast.Token, ty: std.ast.TypeNode) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var pattern_: std.ast.Pattern

    var colon_: std.ast.Token

    var type_: std.ast.TypeNode

    prop pattern: std.ast.Pattern

    prop colon: std.ast.Token

    prop patternType: std.ast.TypeNode

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createEnumPattern(pattern: std.ast.NodeFormat_Pattern): std.ast.EnumPattern

class EnumPattern <: std.ast.Pattern {
    init(constructor: std.ast.Expr, lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var constructor_: std.ast.Expr

    var lParen_: std.ast.Token

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    prop constructor: std.ast.Expr

    prop lParen: std.ast.Token

    prop patterns: std.collection.ArrayList<std.ast.Pattern>

    prop commas: std.ast.Tokens

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func createTuplePattern(pattern: std.ast.NodeFormat_Pattern): std.ast.TuplePattern

class TuplePattern <: std.ast.Pattern {
    init(lParen: std.ast.Token, patterns: std.collection.ArrayList<std.ast.Pattern>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var lParen_: std.ast.Token

    var patterns_: std.collection.ArrayList<std.ast.Pattern>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    prop lParen: std.ast.Token

    prop patterns: std.collection.ArrayList<std.ast.Pattern>

    prop commas: std.ast.Tokens

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

func parseType(input: std.ast.Tokens): std.ast.TypeNode

func parseTypeFragment(input: std.ast.Tokens, startFrom: Int64): (std.ast.TypeNode, Int64)

func generateTypeAST(tyNode: std.core.Option<std.ast.NodeFormat_Type>): std.ast.TypeNode

func createPrimitiveType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createRefType(tyNode: std.core.Option<std.ast.NodeFormat_RefType>, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createQualifiedType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createParenType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createTupleType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createThisType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createFuncType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createVArrayType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

func createPrefixType(tyNode: std.ast.NodeFormat_Type, nodeBase: std.ast.NodeFormat_NodeBase): std.ast.TypeNode

interface ToBytes {
    func toBytes: std.core.Array<UInt8>
}

func getMacroPosition(macroObjPtr: CPointer<Unit>): (UInt32, Int32, Int32)

let SIZE_OF_OFFSET: UInt32

let SIZE_OF_UINT32: UInt32

func castUInt32ToBytes(v: UInt32): std.core.Array<UInt8>

func castInt32ToBytes(v: Int32): std.core.Array<UInt8>

func castUInt16ToBytes(v: UInt16): std.core.Array<UInt8>

func castUInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt32): Int64

func castInt32ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: Int32): Int64

func castUInt16ToBytes(arr: std.core.Array<UInt8>, start: Int64, v: UInt16): Int64

func castStringToBytes(arr: std.core.Array<UInt8>, start1: Int64, s: std.core.String): Int64

func concatBytes(l: std.core.Array<UInt8>, r: std.core.Array<UInt8>): std.core.Array<UInt8>

func castStringToBytes(s: std.core.String): std.core.Array<UInt8>

func refreshPos(token: std.ast.Token, f: UInt32, l: Int32, c: Int32): std.ast.Token

let PATTERNS: std.core.Array<(std.ast.TokenKind, std.core.String, std.core.String)>

func getTokenID(tk: std.ast.TokenKind): UInt16

func getTokenKind(no: UInt16): std.ast.TokenKind

func getNewlineValue: std.core.String

func getTokenKindValue(tk: std.ast.TokenKind): std.core.String

func getTokenLiteral(tk: std.ast.TokenKind): std.core.String

class TokenKindHelper {
    let LIT_CONST_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    let STRING_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    let PRIMITIVE_TYPE_KIND_TO_TOKEN_KIND: std.core.Array<std.ast.TokenKind>

    func getPrimitiveTypeTokenKind(i: UInt16): std.ast.TokenKind

    func getLiteralConstKind(i: UInt16): std.ast.TokenKind

    func getStringKind(i: UInt16): std.ast.TokenKind

    init() { /* compiled code */ }
}

class TokensIterator <: std.core.Iterator<std.ast.Token> {
    var position: Int64

    let data: std.ast.Tokens

    init(tokens: std.ast.Tokens) { /* compiled code */ }

    func peek: std.core.Option<std.ast.Token>

    func seeing(kind: std.ast.TokenKind): Bool

    func next: std.core.Option<std.ast.Token>
}

func refreshTokensPosition(tks: std.ast.Tokens): std.ast.Tokens

func tokenToString(tk: std.ast.Token): std.core.String

class ConcatTokens <: std.ast.Tokens {
    let leftChild: std.ast.Tokens

    var rightChild: std.ast.Tokens

    var cachedSize: Int64

    init(lc: std.ast.Tokens, rc: std.ast.Tokens) { /* compiled code */ }

    prop size: Int64

    func `get`(index: Int64): std.ast.Token

    operator func [](range: std.core.Range<Int64>): std.ast.Tokens

    func append(tks: std.ast.Tokens): std.ast.Tokens

    func append(tk: std.ast.Token): std.ast.Tokens
}

extend Token <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Tokens <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

extend Extend <: ToTokens {
    func toTokens: std.ast.Tokens
}

func countDelimiter(charArray: std.core.Array<Rune>): (UInt16, std.core.String, std.core.String)

extend String <: ToTokens {
    func toTokens: std.ast.Tokens
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

    prop identifier: std.ast.Token

    prop lAngle: std.ast.Token

    prop typeArguments: std.collection.ArrayList<std.ast.TypeNode>

    prop commas: std.ast.Tokens

    prop rAngle: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop baseType: std.ast.TypeNode

    prop dot: std.ast.Token

    prop identifier: std.ast.Token

    prop lAngle: std.ast.Token

    prop typeArguments: std.collection.ArrayList<std.ast.TypeNode>

    prop commas: std.ast.Tokens

    prop rAngle: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ParenType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, parenthesizedType: std.ast.TypeNode, rParen: std.ast.Token) { /* compiled code */ }

    init() { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    var lParen_: std.ast.Token

    var ty_: std.ast.TypeNode

    var rParen_: std.ast.Token

    prop lParen: std.ast.Token

    prop parenthesizedType: std.ast.TypeNode

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class TupleType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, lParen: std.ast.Token, types: std.collection.ArrayList<std.ast.TypeNode>, commas: std.ast.Tokens, rParen: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var lParen_: std.ast.Token

    var types_: std.collection.ArrayList<std.ast.TypeNode>

    var commas_: std.ast.Tokens

    var rParen_: std.ast.Token

    prop lParen: std.ast.Token

    prop types: std.collection.ArrayList<std.ast.TypeNode>

    prop rParen: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class ThisType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, keyword: std.ast.Token) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var keyword_: std.ast.Token

    prop keyword: std.ast.Token

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}

class PrefixType <: std.ast.TypeNode {
    init(typeParameterName: std.ast.Token, colon: std.ast.Token, prefixOps: std.ast.Tokens, baseType: std.ast.TypeNode) { /* compiled code */ }

    init(inputs: std.ast.Tokens) { /* compiled code */ }

    init() { /* compiled code */ }

    var prefixOps_: std.ast.Tokens

    var baseType_: std.ast.TypeNode

    prop prefixOps: std.ast.Tokens

    prop baseType: std.ast.TypeNode

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
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

    prop keyword: std.ast.Token

    prop lParen: std.ast.Token

    prop types: std.collection.ArrayList<std.ast.TypeNode>

    prop commas: std.ast.Tokens

    prop rParen: std.ast.Token

    prop arrow: std.ast.Token

    prop returnType: std.ast.TypeNode

    func toTokens: std.ast.Tokens

    func traverse(v: std.ast.Visitor): Unit

    func dump(indent: UInt16): std.core.String
}
