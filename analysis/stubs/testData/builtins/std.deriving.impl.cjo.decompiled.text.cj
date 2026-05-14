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

interface ClassLikeDecl {
    mut prop body: std.ast.Body {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func allFields(): std.core.Array<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

    func allNamedDeclsImpl(): std.core.Iterator<std.ast.Decl> { /* compiled code */ }

    func allProperties(): std.core.Array<std.ast.PropDecl> { /* compiled code */ }

    func allGenerics(): std.core.Array<std.deriving.api.GenericDecl> { /* compiled code */ }
}

func unwrapParams(ctor: std.ast.PrimaryCtorDecl): std.core.Iterator<std.ast.Decl> { /* compiled code */ }

func hasIdentifier(decl: std.ast.Decl): Bool { /* compiled code */ }

func filterIsInstance(it: std.core.Iterable<F>): std.core.Iterable<T> { /* compiled code */ }

func settingsForClassLike(inputOptions: std.deriving.impl.InputOptions, clazz: std.deriving.impl.ClassLikeDecl, logger: std.deriving.api.Logger): std.deriving.impl.SettingsOutput { /* compiled code */ }

struct InputOptions {
    let origin: std.core.Array<std.deriving.impl.DeriveInterfaceOption>

    let inputNames: std.core.Array<std.deriving.api.QualifiedName>

    let allInterfaceNames: std.core.Array<std.deriving.api.QualifiedName>

    let decl: std.ast.Decl

    let included: std.core.Array<std.deriving.api.Identifier>

    let excluded: std.core.Array<std.deriving.api.Identifier>

    let ordering: std.core.Array<std.deriving.api.Identifier>

    init(origin: std.core.Array<std.deriving.impl.DeriveInterfaceOption>, inputNames: std.core.Array<std.deriving.api.QualifiedName>, allInterfaceNames: std.core.Array<std.deriving.api.QualifiedName>, decl: std.ast.Decl, included: std.core.Array<std.deriving.api.Identifier>, excluded: std.core.Array<std.deriving.api.Identifier>, ordering: std.core.Array<std.deriving.api.Identifier>) { /* compiled code */ }
}

struct SettingsOutput {
    let settings: std.deriving.api.DerivingSettings

    let target: std.deriving.api.DerivingTarget

    init(settings: std.deriving.api.DerivingSettings, target: std.deriving.api.DerivingTarget) { /* compiled code */ }
}

func applyFieldsOrder(allFieldsMap: std.collection.HashMap<std.deriving.api.Identifier, std.deriving.api.TargetNamedAttribute>, userDefinedOrder: std.core.Array<std.deriving.api.Identifier>, includedSet: std.collection.HashSet<std.deriving.api.Identifier>, excludedSet: std.collection.HashSet<std.deriving.api.Identifier>): std.core.Array<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

func filterFields(includedSet: std.collection.HashSet<std.deriving.api.Identifier>, excludedSet: std.collection.HashSet<std.deriving.api.Identifier>): (std.core.Iterable<std.deriving.api.TargetNamedAttribute>) -> std.core.Iterator<std.deriving.api.TargetNamedAttribute> { /* compiled code */ }

func deriveImpl(rawOptions: std.ast.Tokens, input: std.ast.Tokens, decl: std.ast.Decl, includes: std.core.Array<std.deriving.api.Identifier>, excludes: std.core.Array<std.deriving.api.Identifier>, ordering: std.core.Array<std.deriving.api.Identifier>, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

func diagnose(inputOptions: std.deriving.impl.InputOptions, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.core.Array<std.ast.ExtendDecl> { /* compiled code */ }

func settingsForEnum(inputOptions: std.deriving.impl.InputOptions, enumDecl: std.ast.EnumDecl, logger: std.deriving.api.Logger): std.deriving.impl.SettingsOutput { /* compiled code */ }

extend ClassDecl <: ClassLikeDecl {
    func allGenerics(): std.core.Array<std.deriving.api.GenericDecl> { /* compiled code */ }
}

extend StructDecl <: ClassLikeDecl {
    func allGenerics(): std.core.Array<std.deriving.api.GenericDecl> { /* compiled code */ }
}

func mapInterfaceSettings(genericDecls: std.core.Array<std.deriving.api.GenericDecl>, options: std.core.Array<std.deriving.impl.DeriveInterfaceOption>, logger: std.deriving.api.Logger): std.core.Array<std.deriving.api.DerivingInterfaceSettings> { /* compiled code */ }

struct DeriveInterfaceOption <: std.ast.ToTokens & std.core.ToString {
    init(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName) { /* compiled code */ }

    init(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName, whereKeyword: std.ast.Token, excplicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>) { /* compiled code */ }

    prop constraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint> {
        get() { /* compiled code */ }
    }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let interfaceName: std.deriving.api.Identifier

    let resolvedInterface: std.deriving.api.QualifiedName

    let parsedConstraints: std.core.Option<std.deriving.impl.ParsedExplicitConstraints>

    init(interfaceName: std.deriving.api.Identifier, resolvedInterface: std.deriving.api.QualifiedName, parsedConstraints: std.core.Option<std.deriving.impl.ParsedExplicitConstraints>) { /* compiled code */ }
}

func settingsFor(inputOptions: std.deriving.impl.InputOptions, logger: std.deriving.api.Logger): std.core.Option<std.deriving.impl.SettingsOutput> { /* compiled code */ }

func resolveInterface(name: std.deriving.api.Identifier): std.deriving.api.QualifiedName { /* compiled code */ }

struct ParsedGenericConstraint <: std.core.ToString & std.ast.ToTokens {
    func toDerivingGeneric(decl: std.deriving.api.GenericDecl): std.deriving.api.DerivingGenericSettings { /* compiled code */ }

    func toAst(): std.ast.GenericConstraint { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let genericParameter: std.deriving.api.Identifier

    let upperBounds: std.core.Array<std.ast.TypeNode>

    init(genericParameter: std.deriving.api.Identifier, upperBounds: std.core.Array<std.ast.TypeNode>) { /* compiled code */ }
}

struct ParsedExplicitConstraints <: std.core.ToString & std.ast.ToTokens {
    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let whereKeyword: std.ast.Token

    let explicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>

    init(whereKeyword: std.ast.Token, explicitConstraints: std.core.Array<std.deriving.impl.ParsedGenericConstraint>) { /* compiled code */ }
}

func parseDeriveOptions(input: std.ast.Tokens, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

struct DeriveInputParser {
    static func params(input: std.ast.Tokens, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

    static func params(input: std.deriving.impl.PushbackTokensIterator, logger: std.deriving.api.Logger): std.core.Array<std.deriving.impl.DeriveInterfaceOption> { /* compiled code */ }

    let it: std.deriving.impl.PushbackTokensIterator

    let logger: std.deriving.api.Logger
}

class PushbackTokensIterator {
    var index: Int64

    func next(): std.core.Option<std.ast.Token> { /* compiled code */ }

    func peek(): std.core.Option<std.ast.Token> { /* compiled code */ }

    func seeing(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    func pushback(): Unit { /* compiled code */ }

    func last(): std.core.Option<std.ast.Token> { /* compiled code */ }

    func mark(): std.deriving.impl.Mark { /* compiled code */ }

    func rewindTo(mark: std.deriving.impl.Mark): Unit { /* compiled code */ }

    func rewindTo(index: Int64): Unit { /* compiled code */ }

    func slice(start: std.deriving.impl.Mark, until: std.deriving.impl.Mark): std.ast.Tokens { /* compiled code */ }

    let tokens: std.ast.Tokens

    init(tokens: std.ast.Tokens) { /* compiled code */ }
}

struct Mark {
    let markedAt: Int64

    init(markedAt: Int64) { /* compiled code */ }
}

func collectHashMap(it: std.core.Iterable<(K, V)>): std.collection.HashMap<std.deriving.api.Identifier, V> { /* compiled code */ }

func associateBy(key: (T) -> std.deriving.api.Identifier): (std.core.Iterable<T>) -> std.collection.HashMap<std.deriving.api.Identifier, T> { /* compiled code */ }

func mapNotNone(mapper: (F) -> std.core.Option<T>): (std.core.Iterable<F>) -> std.core.Iterable<T> { /* compiled code */ }

struct MapNotNoneIterable <: std.core.Iterable<T> {
    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    let source: std.core.Iterable<F>

    let mapper: (F) -> std.core.Option<T>

    init(source: std.core.Iterable<F>, mapper: (F) -> std.core.Option<T>) { /* compiled code */ }
}

class MapNotNoneIterator <: std.core.Iterator<T> {
    func next(): std.core.Option<T> { /* compiled code */ }

    let source: std.core.Iterator<F>

    let mapper: (F) -> std.core.Option<T>

    init(source: std.core.Iterator<F>, mapper: (F) -> std.core.Option<T>) { /* compiled code */ }
}

func collectTokens(iterator: std.core.Iterable<std.ast.Token>): std.ast.Tokens { /* compiled code */ }
