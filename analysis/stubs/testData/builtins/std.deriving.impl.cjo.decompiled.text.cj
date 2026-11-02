// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.impl

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.resolve.*
import std.ast.Token
import std.deriving.api.Identifier

internal interface ClassLikeDecl {
    public open mut prop body: std.ast.Body

    public open func allFields(): std.core.Array<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

    public open func allNamedDeclsImpl(): std.core.Iterator<std.ast.Decl> { /* compiled code */ }

    public open func allProperties(): std.core.Array<std.ast.PropDecl> { /* compiled code */ }

    public open func allGenerics(): std.core.Array<std.deriving.api.GenericDecl>
}

private func unwrapParams(ctor: std.ast.PrimaryCtorDecl): std.core.Iterator<std.ast.Decl> { /* compiled code */ }

private func hasIdentifier(decl: std.ast.Decl): Bool { /* compiled code */ }

internal func filterIsInstance<F, T>(it: std.core.Iterable<F>): std.core.Iterable<T> { /* compiled code */ }

internal func settingsForClassLike(inputOptions: std.deriving.impl.InputOptions, clazz: std.deriving.impl.ClassLikeDecl, logger: std.deriving.api.Logger): std.deriving.impl.SettingsOutput { /* compiled code */ }

public struct InputOptions {
    public let origin: std.core.Array<std.deriving.impl.DeriveInterfaceOption>

    public let inputNames: std.core.Array<std.deriving.api.QualifiedName>

    public let allInterfaceNames: std.core.Array<std.deriving.api.QualifiedName>

    public let decl: std.ast.Decl

    public let included: std.core.Array<std.deriving.api.Identifier>

    public let excluded: std.core.Array<std.deriving.api.Identifier>

    public let ordering: std.core.Array<std.deriving.api.Identifier>

    InputOptions(origin: std.core.Array<std.deriving.impl.DeriveInterfaceOption>, inputNames: std.core.Array<std.deriving.api.QualifiedName>, allInterfaceNames: std.core.Array<std.deriving.api.QualifiedName>, decl: std.ast.Decl, included: std.core.Array<std.deriving.api.Identifier>, excluded: std.core.Array<std.deriving.api.Identifier>, ordering: std.core.Array<std.deriving.api.Identifier>) { /* compiled code */ }
}

public struct SettingsOutput {
    public let settings: std.deriving.api.DerivingSettings

    public let target: std.deriving.api.DerivingTarget

    SettingsOutput(settings: std.deriving.api.DerivingSettings, target: std.deriving.api.DerivingTarget) { /* compiled code */ }
}

internal func applyFieldsOrder(allFieldsMap: std.collection.HashMap<std.deriving.api.Identifier, std.deriving.api.TargetNamedAttribute>, userDefinedOrder: std.core.Array<std.deriving.api.Identifier>, includedSet: std.collection.HashSet<std.deriving.api.Identifier>, excludedSet: std.collection.HashSet<std.deriving.api.Identifier>): std.core.Array<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

internal func filterFields(includedSet: std.collection.HashSet<std.deriving.api.Identifier>, excludedSet: std.collection.HashSet<std.deriving.api.Identifier>): std.core.Iterator<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

public func deriveImpl(rawOptions: std.ast.Tokens, input: std.ast.Tokens, decl: std.ast.Decl, includes: std.core.Array<std.deriving.api.Identifier>, excludes: std.core.Array<std.deriving.api.Identifier>, ordering: std.core.Array<std.deriving.api.Identifier>, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

public func diagnose(inputOptions: std.deriving.impl.InputOptions, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.core.Array<std.ast.ExtendDecl> { /* compiled code */ }

internal func settingsForEnum(inputOptions: std.deriving.impl.InputOptions, enumDecl: std.ast.EnumDecl, logger: std.deriving.api.Logger): std.deriving.impl.SettingsOutput { /* compiled code */ }

extend std.ast.ClassDecl <: ClassLikeDecl {
    public func allGenerics(): std.core.Array<std.deriving.api.GenericDecl> { /* compiled code */ }
}

extend std.ast.StructDecl <: ClassLikeDecl {
    public func allGenerics(): std.core.Array<std.deriving.api.GenericDecl> { /* compiled code */ }
}

internal func mapInterfaceSettings(genericDecls: std.core.Array<std.deriving.api.GenericDecl>, options: std.core.Array<std.deriving.impl.DeriveInterfaceOption>, logger: std.deriving.api.Logger): std.core.Array<std.deriving.api.DerivingInterfaceSettings> { /* compiled code */ }

public struct DeriveInterfaceOption <: std.ast.ToTokens & std.core.ToString {
    init(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName) { /* compiled code */ }

    init(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName, whereKeyword: std.ast.Token, excplicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>) { /* compiled code */ }

    public prop constraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint> {
        get() { /* compiled code */ }
    }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public let interfaceName: std.deriving.api.Identifier

    public let resolvedInterface: std.deriving.api.QualifiedName

    public let parsedConstraints: std.core.Option<std.deriving.impl.ParsedExplicitConstraints>

    DeriveInterfaceOption(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName, parsedConstraints: std.core.Option<std.deriving.impl.ParsedExplicitConstraints>) { /* compiled code */ }
}

public func settingsFor(inputOptions: std.deriving.impl.InputOptions, logger: std.deriving.api.Logger): std.core.Option<std.deriving.impl.SettingsOutput> { /* compiled code */ }

internal func resolveInterface(name: std.deriving.api.Identifier): std.deriving.api.QualifiedName { /* compiled code */ }

public struct ParsedGenericConstraint <: std.core.ToString & std.ast.ToTokens {
    public func toDerivingGeneric(decl: std.deriving.api.GenericDecl): std.deriving.api.DerivingGenericSettings { /* compiled code */ }

    public func toAst(): std.ast.GenericConstraint { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public let genericParameter: std.deriving.api.Identifier

    public let upperBounds: std.core.Array<std.ast.TypeNode>

    ParsedGenericConstraint(genericParameter: std.deriving.api.Identifier, upperBounds: std.core.Array<std.ast.TypeNode>) { /* compiled code */ }
}

public struct ParsedExplicitConstraints <: std.core.ToString & std.ast.ToTokens {
    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public let whereKeyword: std.ast.Token

    public let explicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>

    ParsedExplicitConstraints(whereKeyword: std.ast.Token, explicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>) { /* compiled code */ }
}

public func parseDeriveOptions(input: std.ast.Tokens, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

public struct DeriveInputParser {
    public static func params(input: std.ast.Tokens, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

    internal static func params(input: std.deriving.impl.PushbackTokensIterator, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

    private let it: std.deriving.impl.PushbackTokensIterator

    private let logger: std.deriving.api.Logger
}

internal class PushbackTokensIterator {
    private var index: Int64

    internal func next(): std.core.Option<std.ast.Token> { /* compiled code */ }

    internal func peek(): std.core.Option<std.ast.Token> { /* compiled code */ }

    internal func seeing(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    internal func pushback(): Unit { /* compiled code */ }

    internal func last(): std.core.Option<std.ast.Token> { /* compiled code */ }

    internal func mark(): std.deriving.impl.Mark { /* compiled code */ }

    internal func rewindTo(mark: std.deriving.impl.Mark): Unit { /* compiled code */ }

    internal func rewindTo(index: Int64): Unit { /* compiled code */ }

    internal func slice(start: std.deriving.impl.Mark, until: std.deriving.impl.Mark): std.ast.Tokens { /* compiled code */ }

    private let tokens: std.ast.Tokens

    PushbackTokensIterator(tokens: std.ast.Tokens) { /* compiled code */ }
}

internal struct Mark {
    internal let markedAt: Int64

    Mark(markedAt: Int64) { /* compiled code */ }
}

internal func collectHashMap<K, V>(it: std.core.Iterable<(K, V)>): std.collection.HashMap<std.deriving.api.Identifier, V> { /* compiled code */ }

internal func associateBy<T>(key: (T) -> std.deriving.api.Identifier): std.collection.HashMap<std.deriving.api.Identifier, T> { /* compiled code */ }

internal func mapNotNone<F, T>(mapper: (F) -> std.core.Option<T>): std.core.Iterable<T> { /* compiled code */ }

internal struct MapNotNoneIterable<F, T> <: std.core.Iterable<T> {
    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    private let source: std.core.Iterable<F>

    private let mapper: (F) -> std.core.Option<T>

    MapNotNoneIterable(source: std.core.Iterable<F>, mapper: (F) -> std.core.Option<T>) { /* compiled code */ }
}

internal class MapNotNoneIterator<F, T> <: std.core.Iterator<T> {
    public func next(): std.core.Option<T> { /* compiled code */ }

    private let source: std.core.Iterator<F>

    private let mapper: (F) -> std.core.Option<T>

    MapNotNoneIterator(source: std.core.Iterator<F>, mapper: (F) -> std.core.Option<T>) { /* compiled code */ }
}

internal func collectTokens(iterator: std.core.Iterable<std.ast.Token>): std.ast.Tokens { /* compiled code */ }
