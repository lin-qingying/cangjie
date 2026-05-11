// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.resolve

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.api.Identifier
import std.convert.Parsable

func resolveFields(decl: std.ast.ClassDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

func resolveFields(decl: std.ast.StructDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

func resolveFields(body: std.ast.Body): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

func blockType(block: std.ast.Block, scope: std.deriving.resolve.Scope): std.ast.TypeNode { /* compiled code */ }

class Scope <: std.core.Object & std.core.Equatable<std.deriving.resolve.Scope> & std.core.ToString {
    let memebers: std.collection.HashMap<std.deriving.api.Identifier, std.ast.TypeNode>

    let invalids: std.collection.HashSet<std.deriving.api.Identifier>

    var scopeType_: std.core.Option<std.ast.TypeNode>

    let id: Int64

    func lookup(name: std.deriving.api.Identifier): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    func lookup(name: std.ast.Token): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    func lookup(name: std.core.String): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    func append(name: std.deriving.api.Identifier): Unit { /* compiled code */ }

    func append(name: std.deriving.api.Identifier, t: std.ast.TypeNode): Unit { /* compiled code */ }

    func appendScopeType(t: std.ast.TypeNode): Unit { /* compiled code */ }

    prop root: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    prop funcScope: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    prop isRoot: Bool {
        get() { /* compiled code */ }
    }

    prop allInvalids: std.core.Array<std.deriving.api.Identifier> {
        get() { /* compiled code */ }
    }

    prop isFuncScope: Bool {
        get() { /* compiled code */ }
    }

    prop scopeType: std.ast.TypeNode {
        get() { /* compiled code */ }
    }

    override operator func ==(other: std.deriving.resolve.Scope): Bool { /* compiled code */ }

    override operator func !=(other: std.deriving.resolve.Scope): Bool { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    func parents(): std.core.Iterable<std.deriving.resolve.Scope> { /* compiled code */ }

    func allItems(): std.core.Iterable<(std.deriving.api.Identifier, std.ast.TypeNode)> { /* compiled code */ }

    let parent: std.core.Option<std.deriving.resolve.Scope>

    let kind: std.deriving.resolve.ScopeKind

    init(parent: std.core.Option<std.deriving.resolve.Scope>, kind: std.deriving.resolve.ScopeKind) { /* compiled code */ }
}

func callType(call: std.ast.CallExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func spawnType(s: std.ast.SpawnExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func flow(nodes: std.core.Iterable<std.ast.Node>, blockScope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func bodyDecl(body: std.ast.Body, classScope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func constructorDecl(c: std.ast.PrimaryCtorDecl, parent: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func funcDecl(f: std.ast.FuncDecl, parent: std.deriving.resolve.Scope): Unit { /* compiled code */ }

extend Scope {
    func appendParams(funcParams: std.core.Iterable<std.ast.FuncParam>): Unit { /* compiled code */ }
}

func varDecl(a: std.ast.VarDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func propDecl(p: std.ast.PropDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func propSetter(p: std.ast.PropDecl, setter: std.ast.FuncDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func assign(assign: std.ast.AssignExpr, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func memberAccess(member: std.ast.MemberAccess, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func memberAccess(ref: std.ast.RefExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func extractOptionType(member: std.ast.MemberAccess): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func funcReturning(t: std.ast.TypeNode): std.ast.FuncType { /* compiled code */ }

func isSome(ref: std.ast.RefExpr): Bool { /* compiled code */ }

func isSome(t: std.ast.Token): Bool { /* compiled code */ }

func isNone(t: std.ast.Token): Bool { /* compiled code */ }

func isOption(ref: std.ast.RefExpr): Bool { /* compiled code */ }

func expressionType(expression: std.ast.Expr): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func constExprTypeOpt(expr: std.core.Option<std.ast.Expr>, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func constExprType(expr: std.ast.Expr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func lambdaType(expr: std.ast.LambdaExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func patternExpr(p: std.ast.Pattern, t: std.ast.TypeNode, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

func optionType(t: std.ast.TypeNode): std.ast.TypeNode { /* compiled code */ }

enum ScopeResolveResult {
    | Unresolved(std.deriving.api.Identifier)

    | InvalidType(std.deriving.api.Identifier, std.deriving.resolve.Scope)

    | ResolvedType(std.deriving.api.Identifier, std.ast.TypeNode, std.deriving.resolve.Scope)

    prop resolvedType: std.core.Option<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }

    prop scope: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    prop id: std.deriving.api.Identifier {
        get() { /* compiled code */ }
    }
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

    static func isSame(a: std.ast.TypeNode, b: std.ast.TypeNode): Bool { /* compiled code */ }

    static func isUnit(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    static func isNothing(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    static func isArray(node: std.ast.TypeNode): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

    static func isIntType(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    static func isFloatType(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    static func isString(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }
}

func unionApprox(a: std.ast.TypeNode, b: std.ast.TypeNode): std.ast.TypeNode { /* compiled code */ }

func unionApprox(a: std.core.Option<std.ast.TypeNode>, b: std.core.Option<std.ast.TypeNode>): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

func typeByKeyword(keyword: std.ast.Token): std.ast.TypeNode { /* compiled code */ }

func myReduce(combine: (T, T) -> T): (std.core.Iterable<T>) -> std.core.Option<T> { /* compiled code */ }

func unopt(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T> { /* compiled code */ }

func myParseType(tokens: std.ast.Tokens): std.ast.TypeNode { /* compiled code */ }
