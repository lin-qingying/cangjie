// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.api

import std.ast.*
import std.collection.*
import std.core.*
import std.collection.ArrayList
import std.unicode.*
import std.sync.AtomicInt64

interface Logger {
    func warning(at: std.ast.Tokens, message: std.core.String): Unit

    func warning(at: std.ast.Token, message: std.core.String): Unit

    func error(at: std.ast.Tokens, message: std.core.String): Unit

    func error(at: std.ast.Token, message: std.core.String): Unit

    func log(at: std.ast.Tokens, message: std.core.String, error: Bool): Unit
}

interface DeclExt {
    prop modifiers: std.collection.ArrayList<std.ast.Modifier>

    func hasModifier(kind: std.ast.TokenKind): Bool

    func hasModifier(predicate: (std.ast.TokenKind) -> Bool): Bool

    func isStatic: Bool

    func isPrivate: Bool

    func unwrap: std.ast.Decl
}

struct QualifiedName <: std.core.ToString & std.core.Equatable<std.deriving.api.QualifiedName> & std.core.Hashable {
    init(tokens: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(name: std.core.String) { /* compiled code */ }

    init(qType: std.ast.QualifiedType) { /* compiled code */ }

    init(simpleName: std.deriving.api.Identifier) { /* compiled code */ }

    func createUnchecked(tokens: std.core.Array<std.ast.Token>): std.deriving.api.QualifiedName

    prop simpleName: std.ast.Token

    func toSimpleNameQN: std.deriving.api.QualifiedName

    prop components: std.core.Array<std.core.String>

    operator func +(other: std.deriving.api.QualifiedName): std.deriving.api.QualifiedName

    operator func +(other: std.core.String): std.deriving.api.QualifiedName

    operator func +(other: std.ast.Token): std.deriving.api.QualifiedName

    operator func +(other: std.deriving.api.Identifier): std.deriving.api.QualifiedName

    func toString: std.core.String

    operator func ==(other: std.deriving.api.QualifiedName): Bool

    operator func !=(other: std.deriving.api.QualifiedName): Bool

    func hashCode: Int64

    let name: std.core.String

    let tokens: std.core.Array<std.ast.Token>
}

struct GenericDecl <: std.core.Equatable<std.deriving.api.GenericDecl> & std.core.Hashable & std.core.ToString {
    init(identifier: std.ast.Token) { /* compiled code */ }

    operator func ==(other: std.deriving.api.GenericDecl): Bool

    operator func !=(other: std.deriving.api.GenericDecl): Bool

    func toString: std.core.String

    func hashCode: Int64

    let identifier: std.deriving.api.Identifier

    init(identifier: std.deriving.api.Identifier) { /* compiled code */ }
}

struct DerivingTarget {
    prop interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>

    prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName>

    func findIntefaceSettings(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.DerivingInterfaceSettings>

    let declaration: std.ast.Decl

    let namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    let enumCases: std.core.Array<std.deriving.api.TargetEnumCase>

    let settings: std.deriving.api.DerivingSettings

    init(declaration: std.ast.Decl, namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, enumCases: std.core.Array<std.deriving.api.TargetEnumCase>, settings: std.deriving.api.DerivingSettings) { /* compiled code */ }
}

interface GenericsInjector {
    func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, target: std.deriving.api.DerivingTarget): std.ast.TypeNode

    func constraintsFor(iface: std.deriving.api.QualifiedName, generic: std.deriving.api.GenericDecl, target: std.deriving.api.DerivingTarget): std.core.Array<std.ast.GenericConstraint>

    func noGenerics: std.deriving.api.GenericsInjector

    func singleRecursive: std.deriving.api.GenericsInjector
}

struct DefaultGenreicInjector <: std.deriving.api.GenericsInjector {
    func injectGenerics(input: std.deriving.api.QualifiedName, _: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode

    init() { /* compiled code */ }
}

struct RecursiveGenericInjector <: std.deriving.api.GenericsInjector {
    func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode

    init() { /* compiled code */ }
}

struct DerivingSettings {
    prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName>

    let interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>

    let excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    let casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>

    let fieldsOrder: std.core.Array<std.deriving.api.Identifier>

    let includedProperties: std.core.Array<std.ast.PropDecl>

    init(interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>, excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>, fieldsOrder: std.core.Array<std.deriving.api.Identifier>, includedProperties: std.core.Array<std.ast.PropDecl>) { /* compiled code */ }
}

struct DerivingInterfaceSettings {
    let name: std.deriving.api.QualifiedName

    let generics: std.core.Array<std.deriving.api.DerivingGenericSettings>

    init(name: std.deriving.api.QualifiedName, generics: std.core.Array<std.deriving.api.DerivingGenericSettings>) { /* compiled code */ }
}

enum TargetNamedAttribute {
    | FieldAttribute(std.ast.VarDecl)

    | ConstructorFieldAttribute(std.ast.FuncParam)

    | PropertyAttribute(std.ast.PropDecl)
}

struct TargetEnumCase {
    let name: std.core.String

    let constructor: std.ast.Constructor

    let fields: std.core.Array<std.deriving.api.TargetEnumField>

    init(name: std.core.String, constructor: std.ast.Constructor, fields: std.core.Array<std.deriving.api.TargetEnumField>) { /* compiled code */ }
}

struct Identifier <: std.core.ToString & std.core.Equatable<std.deriving.api.Identifier> & std.core.Hashable & std.ast.ToTokens {
    init(str: std.core.String) { /* compiled code */ }

    prop value: std.core.String

    func toString: std.core.String

    operator func ==(other: std.deriving.api.Identifier): Bool

    operator func !=(other: std.deriving.api.Identifier): Bool

    func hashCode: Int64

    func toTokens: std.ast.Tokens

    let token: std.ast.Token

    init(token: std.ast.Token) { /* compiled code */ }
}

struct TargetEnumField {
    let position: Int64

    let fieldType: std.ast.TypeNode

    init(position: Int64, fieldType: std.ast.TypeNode) { /* compiled code */ }
}

struct DerivingGenericSettings <: std.core.ToString & std.ast.ToTokens {
    prop hasUpperBounds: Bool

    func toNode: std.ast.GenericConstraint

    func toTokens: std.ast.Tokens

    func toString: std.core.String

    let generic: std.deriving.api.GenericDecl

    let upperBounds: std.core.Array<std.ast.TypeNode>

    init(generic: std.deriving.api.GenericDecl, upperBounds: std.core.Array<std.ast.TypeNode>) { /* compiled code */ }
}

interface DerivingNode <: std.deriving.api.AstExt {
    func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit

    func appendAllConstraintsTo(deriving: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit
}

interface Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(input: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens
}

struct DerivingInterfaceInfo {
    let name: std.deriving.api.QualifiedName

    let superTypes: std.core.Array<std.deriving.api.QualifiedName>

    let genericsInjector: std.deriving.api.GenericsInjector

    init(name: std.deriving.api.QualifiedName, superTypes: std.core.Array<std.deriving.api.QualifiedName>, genericsInjector: std.deriving.api.GenericsInjector) { /* compiled code */ }
}

interface AstExt {
    func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit

    func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit

    func appendFunction(f: std.ast.FuncDecl): Unit

    func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl
}

extend DerivingTarget {
    func createExtendSkeleton: std.ast.ExtendDecl

    func appendAllConstraintsTo(ext: std.ast.ExtendDecl, deriving: std.deriving.api.Deriving, iface: std.deriving.api.QualifiedName): Unit

    func appendAllConstraintsTo(ext: std.ast.ExtendDecl, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit

    func upperBoundForExtend(iface: std.deriving.api.DerivingInterfaceInfo): std.ast.TypeNode

    func genericConstraints(ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): std.core.Iterable<std.ast.GenericConstraint>
}

extend ExtendDecl <: DerivingNode {
    func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit

    func appendAllConstraintsTo(target: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit
}

extend DerivingTarget {
    prop isNonFinal: Bool

    prop isFinal: Bool

    prop isSealedClass: Bool

    prop isAbstractClass: Bool

    prop allGenerics: std.core.Array<std.deriving.api.GenericDecl>

    prop hasAnyGenerics: Bool

    prop superTypes: std.core.Array<std.ast.TypeNode>

    prop superTypeNames: std.core.Array<std.deriving.api.QualifiedName>

    func toType: std.ast.TypeNode

    func ifClass(predicate: (std.ast.ClassDecl) -> Bool): Bool

    func generics: std.core.Option<std.ast.GenericParam>
}

extend TargetNamedAttribute {
    prop decl: std.ast.Decl

    prop identifier: std.deriving.api.Identifier

    prop typeNode: std.core.Option<std.ast.TypeNode>
}

extend TargetNamedAttribute <: ToString {
    func toString: std.core.String
}

extend TargetEnumCase {
    prop totalArgumentsCount: Int64

    prop components: std.core.Array<std.core.Option<std.deriving.api.TargetEnumField>>
}

extend GenericDecl {
    func toType: std.ast.RefType
}

extend Identifier {
    func toType: std.ast.RefType
}

class CollectingLogger <: std.core.Object & std.deriving.api.Logger {
    let elements: std.collection.ArrayList<std.core.String>

    func log(at: std.ast.Tokens, message: std.core.String, error: Bool): Unit

    func collect: std.core.Array<std.core.String>

    func clear: Unit

    init() { /* compiled code */ }
}

interface AppendGenerics {
    func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit
}

extend RefType <: AppendGenerics {
    func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit
}

extend QualifiedType <: AppendGenerics {
    func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit
}

interface BodyExt {
    func declsUnwrapped: std.core.Iterator<std.ast.Decl>
}

interface FuncParamExt {
    func unwrapParam: std.ast.FuncParam
}

interface FunctionExt {
    func funcParamsUnwrapped: std.core.Iterator<std.ast.FuncParam>
}

extend Decl <: DeclExt {
    func unwrap: std.ast.Decl
}

extend Body <: BodyExt {
    func declsUnwrapped: std.core.Iterator<std.ast.Decl>
}

extend FuncParam <: FuncParamExt {
    func unwrapParam: std.ast.FuncParam
}

extend FuncDecl <: FunctionExt {
    func funcParamsUnwrapped: std.core.Iterator<std.ast.FuncParam>
}

extend PrimaryCtorDecl <: FunctionExt {
    func funcParamsUnwrapped: std.core.Iterator<std.ast.FuncParam>
}

extend ExtendDecl <: AstExt {
    func appendFunction(f: std.ast.FuncDecl): Unit

    func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl

    func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit

    func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit
}

func joinTokens(prefix: std.ast.ToTokens, postfix: std.ast.ToTokens, delimiter: std.ast.ToTokens, ifEmpty: std.ast.ToTokens): (std.core.Iterable<std.ast.Tokens>) -> std.ast.Tokens

extend QualifiedName {
    func toType(genericArgs: std.core.Array<std.ast.TypeNode>): std.ast.TypeNode
}

func registerDeriving(implementation: std.deriving.api.Deriving): Unit

func findDeriving(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Deriving>

func findDerivings(names: std.core.Iterable<std.deriving.api.QualifiedName>): (std.core.Array<(std.deriving.api.Deriving, std.core.Array<std.deriving.api.QualifiedName>)>, std.core.Array<std.deriving.api.QualifiedName>)

func interfacesBySimpleName(simpleName: std.core.String): std.core.Array<std.deriving.api.QualifiedName>

struct Registration <: std.core.Equatable<std.deriving.api.Registration> & std.core.Hashable {
    let id: Int64

    func register(name: std.deriving.api.QualifiedName, candidate: std.deriving.api.Registration): Unit

    func find(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Registration>

    func find(simpleName: std.core.String): std.core.Array<std.deriving.api.Registration>

    operator func ==(other: std.deriving.api.Registration): Bool

    operator func !=(other: std.deriving.api.Registration): Bool

    func hashCode: Int64

    let deriving: std.deriving.api.Deriving

    init(deriving: std.deriving.api.Deriving) { /* compiled code */ }
}
