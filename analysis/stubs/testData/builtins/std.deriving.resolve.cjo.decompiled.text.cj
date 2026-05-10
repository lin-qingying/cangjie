// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.resolve

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.api.Identifier
import std.convert.Parsable

func resolveFields(decl: std.ast.ClassDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)>

func resolveFields(decl: std.ast.StructDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)>

func resolveFields(body: std.ast.Body): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)>

func blockType(block: std.ast.Block, scope: std.deriving.resolve.Scope): std.ast.TypeNode

class Scope <: std.core.Object & std.core.Equatable<std.deriving.resolve.Scope> & std.core.ToString {
    let memebers: std.collection.HashMap<std.deriving.api.Identifier, std.ast.TypeNode>

    let invalids: std.collection.HashSet<std.deriving.api.Identifier>

    var scopeType_: std.core.Option<std.ast.TypeNode>

    let id: Int64

    func lookup(name: std.deriving.api.Identifier): std.deriving.resolve.ScopeResolveResult

    func lookup(name: std.ast.Token): std.deriving.resolve.ScopeResolveResult

    func lookup(name: std.core.String): std.deriving.resolve.ScopeResolveResult

    func append(name: std.deriving.api.Identifier): Unit

    func append(name: std.deriving.api.Identifier, t: std.ast.TypeNode): Unit

    func appendScopeType(t: std.ast.TypeNode): Unit

    prop root: std.core.Option<std.deriving.resolve.Scope>

    prop funcScope: std.core.Option<std.deriving.resolve.Scope>

    prop isRoot: Bool

    prop allInvalids: std.core.Array<std.deriving.api.Identifier>

    prop isFuncScope: Bool

    prop scopeType: std.ast.TypeNode

    operator func ==(other: std.deriving.resolve.Scope): Bool

    operator func !=(other: std.deriving.resolve.Scope): Bool

    func toString: std.core.String

    func parents: std.core.Iterable<std.deriving.resolve.Scope>

    func allItems: std.core.Iterable<(std.deriving.api.Identifier, std.ast.TypeNode)>

    let parent: std.core.Option<std.deriving.resolve.Scope>

    let kind: std.deriving.resolve.ScopeKind

    init(parent: std.core.Option<std.deriving.resolve.Scope>, kind: std.deriving.resolve.ScopeKind) { /* compiled code */ }
}

func callType(call: std.ast.CallExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func spawnType(s: std.ast.SpawnExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func flow(nodes: std.core.Iterable<std.ast.Node>, blockScope: std.deriving.resolve.Scope): Unit

func bodyDecl(body: std.ast.Body, classScope: std.deriving.resolve.Scope): Unit

func constructorDecl(c: std.ast.PrimaryCtorDecl, parent: std.deriving.resolve.Scope): Unit

func funcDecl(f: std.ast.FuncDecl, parent: std.deriving.resolve.Scope): Unit

extend Scope {
    func appendParams(funcParams: std.core.Iterable<std.ast.FuncParam>): Unit
}

func varDecl(a: std.ast.VarDecl, scope: std.deriving.resolve.Scope): Unit

func propDecl(p: std.ast.PropDecl, scope: std.deriving.resolve.Scope): Unit

func propSetter(p: std.ast.PropDecl, setter: std.ast.FuncDecl, scope: std.deriving.resolve.Scope): Unit

func assign(assign: std.ast.AssignExpr, scope: std.deriving.resolve.Scope): Unit

func memberAccess(member: std.ast.MemberAccess, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func memberAccess(ref: std.ast.RefExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func extractOptionType(member: std.ast.MemberAccess): std.core.Option<std.ast.TypeNode>

func funcReturning(t: std.ast.TypeNode): std.ast.FuncType

func isSome(ref: std.ast.RefExpr): Bool

func isSome(t: std.ast.Token): Bool

func isNone(t: std.ast.Token): Bool

func isOption(ref: std.ast.RefExpr): Bool

func expressionType(expression: std.ast.Expr): std.core.Option<std.ast.TypeNode>

func constExprTypeOpt(expr: std.core.Option<std.ast.Expr>, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func constExprType(expr: std.ast.Expr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func lambdaType(expr: std.ast.LambdaExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode>

func patternExpr(p: std.ast.Pattern, t: std.ast.TypeNode, scope: std.deriving.resolve.Scope): Unit

func optionType(t: std.ast.TypeNode): std.ast.TypeNode

enum ScopeResolveResult {
    | Unresolved(std.deriving.api.Identifier)

    | InvalidType(std.deriving.api.Identifier, std.deriving.resolve.Scope)

    | ResolvedType(std.deriving.api.Identifier, std.ast.TypeNode, std.deriving.resolve.Scope)

    prop resolvedType: std.core.Option<std.ast.TypeNode>

    prop scope: std.core.Option<std.deriving.resolve.Scope>

    prop id: std.deriving.api.Identifier
}

enum ScopeKind {
    | FuncScope

    | ClassLikeScope

    | OtherScope
}

struct Types {
    let SuppressUnusedInit: std.deriving.resolve.Types

    let AnyType: std.ast.TypeNode

    let NothingType: std.ast.TypeNode

    let UnitType: std.ast.TypeNode

    let BoolType: std.ast.TypeNode

    let TokensType: std.ast.TypeNode

    let ByteType: std.ast.TypeNode

    let Int64Type: std.ast.TypeNode

    let Float64Type: std.ast.TypeNode

    let StringType: std.ast.TypeNode

    let ByteArrayType: std.ast.TypeNode

    let CharType: std.ast.TypeNode

    func isSame(a: std.ast.TypeNode, b: std.ast.TypeNode): Bool

    func isUnit(typeNode: std.ast.TypeNode): Bool

    func isNothing(typeNode: std.ast.TypeNode): Bool

    func isArray(node: std.ast.TypeNode): std.core.Option<std.ast.TypeNode>

    func isIntType(typeNode: std.ast.TypeNode): Bool

    func isFloatType(typeNode: std.ast.TypeNode): Bool

    func isString(typeNode: std.ast.TypeNode): Bool
}

func unionApprox(a: std.ast.TypeNode, b: std.ast.TypeNode): std.ast.TypeNode

func unionApprox(a: std.core.Option<std.ast.TypeNode>, b: std.core.Option<std.ast.TypeNode>): std.core.Option<std.ast.TypeNode>

func typeByKeyword(keyword: std.ast.Token): std.ast.TypeNode

func myReduce(combine: (T, T) -> T): (std.core.Iterable<T>) -> std.core.Option<T>

func unopt(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T>

func myParseType(tokens: std.ast.Tokens): std.ast.TypeNode
