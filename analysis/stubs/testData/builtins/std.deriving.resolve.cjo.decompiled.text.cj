// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.resolve

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.api.Identifier
import std.convert.Parsable

public func resolveFields(decl: std.ast.ClassDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

public func resolveFields(decl: std.ast.StructDecl): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

internal func resolveFields(body: std.ast.Body): std.core.Array<(std.ast.VarDecl, std.core.Option<std.ast.TypeNode>)> { /* compiled code */ }

internal func blockType(block: std.ast.Block, scope: std.deriving.resolve.Scope): std.ast.TypeNode { /* compiled code */ }

public class Scope <: std.core.Object & std.core.Equatable<std.deriving.resolve.Scope> & std.core.ToString {
    private let memebers: std.collection.HashMap<std.deriving.api.Identifier, std.ast.TypeNode>

    private let invalids: std.collection.HashSet<std.deriving.api.Identifier>

    private var scopeType_: std.core.Option<std.ast.TypeNode>

    private let id: Int64

    public func lookup(name: std.deriving.api.Identifier): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    public func lookup(name: std.ast.Token): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    public func lookup(name: std.core.String): std.deriving.resolve.ScopeResolveResult { /* compiled code */ }

    public func append(name: std.deriving.api.Identifier): Unit { /* compiled code */ }

    public func append(name: std.deriving.api.Identifier, t: std.ast.TypeNode): Unit { /* compiled code */ }

    public func appendScopeType(t: std.ast.TypeNode): Unit { /* compiled code */ }

    public prop root: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    public prop funcScope: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    public prop isRoot: Bool {
        get() { /* compiled code */ }
    }

    public prop allInvalids: std.core.Array<std.deriving.api.Identifier> {
        get() { /* compiled code */ }
    }

    internal prop isFuncScope: Bool {
        get() { /* compiled code */ }
    }

    public prop scopeType: std.ast.TypeNode {
        get() { /* compiled code */ }
    }

    public override operator func ==(other: std.deriving.resolve.Scope): Bool { /* compiled code */ }

    public override operator func !=(other: std.deriving.resolve.Scope): Bool { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    internal func parents(): std.core.Iterable<std.deriving.resolve.Scope> { /* compiled code */ }

    internal func allItems(): std.core.Iterable<(std.deriving.api.Identifier, std.ast.TypeNode)> { /* compiled code */ }

    public let parent: std.core.Option<std.deriving.resolve.Scope>

    public let kind: std.deriving.resolve.ScopeKind

    Scope(parent: std.core.Option<std.deriving.resolve.Scope>, kind!: std.deriving.resolve.ScopeKind = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal func callType(call: std.ast.CallExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func spawnType(s: std.ast.SpawnExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func flow(nodes: std.core.Iterable<std.ast.Node>, blockScope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

public func bodyDecl(body: std.ast.Body, classScope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func constructorDecl(c: std.ast.PrimaryCtorDecl, parent: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func funcDecl(f: std.ast.FuncDecl, parent: std.deriving.resolve.Scope): Unit { /* compiled code */ }

extend std.deriving.resolve.Scope {
    internal func appendParams(funcParams: std.core.Iterable<std.ast.FuncParam>): Unit { /* compiled code */ }
}

internal func varDecl(a: std.ast.VarDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func propDecl(p: std.ast.PropDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func propSetter(p: std.ast.PropDecl, setter: std.ast.FuncDecl, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func assign(assign: std.ast.AssignExpr, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func memberAccess(member: std.ast.MemberAccess, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func memberAccess(ref: std.ast.RefExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func extractOptionType(member: std.ast.MemberAccess): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func funcReturning(t: std.ast.TypeNode): std.ast.FuncType { /* compiled code */ }

internal func isSome(ref: std.ast.RefExpr): Bool { /* compiled code */ }

internal func isSome(t: std.ast.Token): Bool { /* compiled code */ }

internal func isNone(t: std.ast.Token): Bool { /* compiled code */ }

internal func isOption(ref: std.ast.RefExpr): Bool { /* compiled code */ }

public func expressionType(expression: std.ast.Expr): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func constExprTypeOpt(expr: std.core.Option<std.ast.Expr>, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

public func constExprType(expr: std.ast.Expr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func lambdaType(expr: std.ast.LambdaExpr, scope: std.deriving.resolve.Scope): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func patternExpr(p: std.ast.Pattern, t: std.ast.TypeNode, scope: std.deriving.resolve.Scope): Unit { /* compiled code */ }

internal func optionType(t: std.ast.TypeNode): std.ast.TypeNode { /* compiled code */ }

public enum ScopeResolveResult {
    | Unresolved(std.deriving.api.Identifier)

    | InvalidType(std.deriving.api.Identifier, std.deriving.resolve.Scope)

    | ResolvedType(std.deriving.api.Identifier, std.ast.TypeNode, std.deriving.resolve.Scope)

    public prop resolvedType: std.core.Option<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }

    public prop scope: std.core.Option<std.deriving.resolve.Scope> {
        get() { /* compiled code */ }
    }

    public prop id: std.deriving.api.Identifier {
        get() { /* compiled code */ }
    }
}

public enum ScopeKind {
    | FuncScope

    | ClassLikeScope

    | OtherScope
}

public struct Types {
    public static let SuppressUnusedInit: std.deriving.resolve.Types

    public static let AnyType: std.ast.TypeNode

    public static let NothingType: std.ast.TypeNode

    public static let UnitType: std.ast.TypeNode

    public static let BoolType: std.ast.TypeNode

    public static let TokensType: std.ast.TypeNode

    public static let ByteType: std.ast.TypeNode

    public static let Int64Type: std.ast.TypeNode

    public static let Float64Type: std.ast.TypeNode

    public static let StringType: std.ast.TypeNode

    public static let ByteArrayType: std.ast.TypeNode

    public static let CharType: std.ast.TypeNode

    public static func isSame(a: std.ast.TypeNode, b: std.ast.TypeNode): Bool { /* compiled code */ }

    public static func isUnit(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    public static func isNothing(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    public static func isArray(node: std.ast.TypeNode): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

    public static func isIntType(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    public static func isFloatType(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }

    public static func isString(typeNode: std.ast.TypeNode): Bool { /* compiled code */ }
}

public func unionApprox(a: std.ast.TypeNode, b: std.ast.TypeNode): std.ast.TypeNode { /* compiled code */ }

public func unionApprox(a: std.core.Option<std.ast.TypeNode>, b: std.core.Option<std.ast.TypeNode>): std.core.Option<std.ast.TypeNode> { /* compiled code */ }

internal func typeByKeyword(keyword: std.ast.Token): std.ast.TypeNode { /* compiled code */ }

internal func myReduce<T>(combine: (T, T) -> T): std.core.Option<T> { /* compiled code */ }

internal func unopt<T>(opt: std.core.Option<std.core.Option<T>>): std.core.Option<T> { /* compiled code */ }

internal func myParseType(tokens: std.ast.Tokens): std.ast.TypeNode { /* compiled code */ }
