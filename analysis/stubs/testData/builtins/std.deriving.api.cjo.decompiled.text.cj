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
    func warning(at: std.ast.Tokens, message: std.core.String): Unit { /* compiled code */ }

    func warning(at: std.ast.Token, message: std.core.String): Unit { /* compiled code */ }

    func error(at: std.ast.Tokens, message: std.core.String): Unit { /* compiled code */ }

    func error(at: std.ast.Token, message: std.core.String): Unit { /* compiled code */ }

    abstract func log(at: std.ast.Tokens, message: std.core.String, error: Bool): Unit
}

interface DeclExt {
    abstract mut prop modifiers: std.collection.ArrayList<std.ast.Modifier>

    func hasModifier(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    func hasModifier(predicate: (std.ast.TokenKind) -> Bool): Bool { /* compiled code */ }

    func isStatic(): Bool { /* compiled code */ }

    func isPrivate(): Bool { /* compiled code */ }

    abstract func unwrap(): std.ast.Decl
}

struct QualifiedName <: std.core.ToString & std.core.Equatable<std.deriving.api.QualifiedName> & std.core.Hashable {
    init(tokens: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(name: std.core.String) { /* compiled code */ }

    init(qType: std.ast.QualifiedType) { /* compiled code */ }

    init(simpleName: std.deriving.api.Identifier) { /* compiled code */ }

    static func createUnchecked(tokens: std.core.Array<std.ast.Token>): std.deriving.api.QualifiedName { /* compiled code */ }

    prop simpleName: std.ast.Token {
        get() { /* compiled code */ }
    }

    func toSimpleNameQN(): std.deriving.api.QualifiedName { /* compiled code */ }

    prop components: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    operator func +(other: std.deriving.api.QualifiedName): std.deriving.api.QualifiedName { /* compiled code */ }

    operator func +(other: std.core.String): std.deriving.api.QualifiedName { /* compiled code */ }

    operator func +(other: std.ast.Token): std.deriving.api.QualifiedName { /* compiled code */ }

    operator func +(other: std.deriving.api.Identifier): std.deriving.api.QualifiedName { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    override operator func ==(other: std.deriving.api.QualifiedName): Bool { /* compiled code */ }

    override operator func !=(other: std.deriving.api.QualifiedName): Bool { /* compiled code */ }

    override func hashCode(): Int64 { /* compiled code */ }

    let name: std.core.String

    let tokens: std.core.Array<std.ast.Token>
}

struct GenericDecl <: std.core.Equatable<std.deriving.api.GenericDecl> & std.core.Hashable & std.core.ToString {
    init(identifier: std.ast.Token) { /* compiled code */ }

    override operator func ==(other: std.deriving.api.GenericDecl): Bool { /* compiled code */ }

    override operator func !=(other: std.deriving.api.GenericDecl): Bool { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }

    override func hashCode(): Int64 { /* compiled code */ }

    let identifier: std.deriving.api.Identifier

    GenericDecl(identifier: std.deriving.api.Identifier) { /* compiled code */ }
}

struct DerivingTarget {
    prop interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings> {
        get() { /* compiled code */ }
    }

    prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    func findIntefaceSettings(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.DerivingInterfaceSettings> { /* compiled code */ }

    let declaration: std.ast.Decl

    let namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    let enumCases: std.core.Array<std.deriving.api.TargetEnumCase>

    let settings: std.deriving.api.DerivingSettings

    DerivingTarget(declaration: std.ast.Decl, namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, enumCases: std.core.Array<std.deriving.api.TargetEnumCase>, settings: std.deriving.api.DerivingSettings) { /* compiled code */ }
}

interface GenericsInjector {
    abstract func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, target: std.deriving.api.DerivingTarget): std.ast.TypeNode

    func constraintsFor(iface: std.deriving.api.QualifiedName, generic: std.deriving.api.GenericDecl, target: std.deriving.api.DerivingTarget): std.core.Array<std.ast.GenericConstraint> { /* compiled code */ }

    static func noGenerics(): std.deriving.api.GenericsInjector { /* compiled code */ }

    static func singleRecursive(): std.deriving.api.GenericsInjector { /* compiled code */ }
}

struct DefaultGenreicInjector <: std.deriving.api.GenericsInjector {
    func injectGenerics(input: std.deriving.api.QualifiedName, _: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode { /* compiled code */ }

    init() { /* compiled code */ }
}

struct RecursiveGenericInjector <: std.deriving.api.GenericsInjector {
    func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode { /* compiled code */ }

    init() { /* compiled code */ }
}

struct DerivingSettings {
    prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    let interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>

    let excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    let casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>

    let fieldsOrder: std.core.Array<std.deriving.api.Identifier>

    let includedProperties: std.core.Array<std.ast.PropDecl>

    DerivingSettings(interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>, excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>, fieldsOrder: std.core.Array<std.deriving.api.Identifier>, includedProperties: std.core.Array<std.ast.PropDecl>) { /* compiled code */ }
}

struct DerivingInterfaceSettings {
    let name: std.deriving.api.QualifiedName

    let generics: std.core.Array<std.deriving.api.DerivingGenericSettings>

    DerivingInterfaceSettings(name: std.deriving.api.QualifiedName, generics: std.core.Array<std.deriving.api.DerivingGenericSettings>) { /* compiled code */ }
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

    TargetEnumCase(name: std.core.String, constructor: std.ast.Constructor, fields: std.core.Array<std.deriving.api.TargetEnumField>) { /* compiled code */ }
}

struct Identifier <: std.core.ToString & std.core.Equatable<std.deriving.api.Identifier> & std.core.Hashable & std.ast.ToTokens {
    init(str: std.core.String) { /* compiled code */ }

    prop value: std.core.String {
        get() { /* compiled code */ }
    }

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(other: std.deriving.api.Identifier): Bool { /* compiled code */ }

    operator func !=(other: std.deriving.api.Identifier): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    let token: std.ast.Token

    Identifier(token: std.ast.Token) { /* compiled code */ }
}

struct TargetEnumField {
    let position: Int64

    let fieldType: std.ast.TypeNode

    TargetEnumField(position: Int64, fieldType: std.ast.TypeNode) { /* compiled code */ }
}

struct DerivingGenericSettings <: std.core.ToString & std.ast.ToTokens {
    prop hasUpperBounds: Bool {
        get() { /* compiled code */ }
    }

    func toNode(): std.ast.GenericConstraint { /* compiled code */ }

    func toTokens(): std.ast.Tokens { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let generic: std.deriving.api.GenericDecl

    let upperBounds: std.core.Array<std.ast.TypeNode>

    DerivingGenericSettings(generic: std.deriving.api.GenericDecl, upperBounds: std.core.Array<std.ast.TypeNode>) { /* compiled code */ }
}

interface DerivingNode <: std.deriving.api.AstExt {
    abstract func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit

    abstract func appendAllConstraintsTo(deriving: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit
}

interface Deriving {
    abstract prop name: std.core.String

    abstract prop description: std.core.String

    abstract prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    abstract func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    abstract func derive(input: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens
}

struct DerivingInterfaceInfo {
    let name: std.deriving.api.QualifiedName

    let superTypes: std.core.Array<std.deriving.api.QualifiedName>

    let genericsInjector: std.deriving.api.GenericsInjector

    DerivingInterfaceInfo(name: std.deriving.api.QualifiedName, superTypes: std.core.Array<std.deriving.api.QualifiedName>, genericsInjector: std.deriving.api.GenericsInjector) { /* compiled code */ }
}

interface AstExt {
    abstract func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit

    abstract func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit

    abstract func appendFunction(f: std.ast.FuncDecl): Unit

    abstract func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl
}

extend DerivingTarget {
    func createExtendSkeleton(): std.ast.ExtendDecl { /* compiled code */ }

    func appendAllConstraintsTo(ext: std.ast.ExtendDecl, deriving: std.deriving.api.Deriving, iface: std.deriving.api.QualifiedName): Unit { /* compiled code */ }

    func appendAllConstraintsTo(ext: std.ast.ExtendDecl, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit { /* compiled code */ }

    func upperBoundForExtend(iface: std.deriving.api.DerivingInterfaceInfo): std.ast.TypeNode { /* compiled code */ }

    func genericConstraints(ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): std.core.Iterable<std.ast.GenericConstraint> { /* compiled code */ }
}

extend ExtendDecl <: DerivingNode {
    func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit { /* compiled code */ }

    func appendAllConstraintsTo(target: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit { /* compiled code */ }
}

extend DerivingTarget {
    prop isNonFinal: Bool {
        get() { /* compiled code */ }
    }

    prop isFinal: Bool {
        get() { /* compiled code */ }
    }

    prop isSealedClass: Bool {
        get() { /* compiled code */ }
    }

    prop isAbstractClass: Bool {
        get() { /* compiled code */ }
    }

    prop allGenerics: std.core.Array<std.deriving.api.GenericDecl> {
        get() { /* compiled code */ }
    }

    prop hasAnyGenerics: Bool {
        get() { /* compiled code */ }
    }

    prop superTypes: std.core.Array<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }

    prop superTypeNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    func toType(): std.ast.TypeNode { /* compiled code */ }

    func ifClass(predicate: (std.ast.ClassDecl) -> Bool): Bool { /* compiled code */ }

    func generics(): std.core.Option<std.ast.GenericParam> { /* compiled code */ }
}

extend TargetNamedAttribute {
    prop decl: std.ast.Decl {
        get() { /* compiled code */ }
    }

    prop identifier: std.deriving.api.Identifier {
        get() { /* compiled code */ }
    }

    prop typeNode: std.core.Option<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }
}

extend TargetNamedAttribute <: ToString {
    func toString(): std.core.String { /* compiled code */ }
}

extend TargetEnumCase {
    prop totalArgumentsCount: Int64 {
        get() { /* compiled code */ }
    }

    prop components: std.core.Array<std.core.Option<std.deriving.api.TargetEnumField>> {
        get() { /* compiled code */ }
    }
}

extend GenericDecl {
    func toType(): std.ast.RefType { /* compiled code */ }
}

extend Identifier {
    func toType(): std.ast.RefType { /* compiled code */ }
}

class CollectingLogger <: std.core.Object & std.deriving.api.Logger {
    let elements: std.collection.ArrayList<std.core.String>

    override func log(at: std.ast.Tokens, message: std.core.String, error: Bool): Unit { /* compiled code */ }

    func collect(): std.core.Array<std.core.String> { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

interface AppendGenerics {
    abstract func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit
}

extend RefType <: AppendGenerics {
    func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit { /* compiled code */ }
}

extend QualifiedType <: AppendGenerics {
    func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit { /* compiled code */ }
}

interface BodyExt {
    abstract func declsUnwrapped(): std.core.Iterator<std.ast.Decl>
}

interface FuncParamExt {
    abstract func unwrapParam(): std.ast.FuncParam
}

interface FunctionExt {
    abstract func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam>
}

extend Decl <: DeclExt {
    func unwrap(): std.ast.Decl { /* compiled code */ }
}

extend Body <: BodyExt {
    func declsUnwrapped(): std.core.Iterator<std.ast.Decl> { /* compiled code */ }
}

extend FuncParam <: FuncParamExt {
    func unwrapParam(): std.ast.FuncParam { /* compiled code */ }
}

extend FuncDecl <: FunctionExt {
    func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam> { /* compiled code */ }
}

extend PrimaryCtorDecl <: FunctionExt {
    func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam> { /* compiled code */ }
}

extend ExtendDecl <: AstExt {
    func appendFunction(f: std.ast.FuncDecl): Unit { /* compiled code */ }

    func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl { /* compiled code */ }

    func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit { /* compiled code */ }

    func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit { /* compiled code */ }
}

func joinTokens(prefix: std.ast.ToTokens, postfix: std.ast.ToTokens, delimiter: std.ast.ToTokens, ifEmpty: std.ast.ToTokens): (std.core.Iterable<std.ast.Tokens>) -> std.ast.Tokens { /* compiled code */ }

extend QualifiedName {
    func toType(genericArgs: std.core.Array<std.ast.TypeNode>): std.ast.TypeNode { /* compiled code */ }
}

func registerDeriving(implementation: std.deriving.api.Deriving): Unit { /* compiled code */ }

func findDeriving(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Deriving> { /* compiled code */ }

func findDerivings(names: std.core.Iterable<std.deriving.api.QualifiedName>): (std.core.Array<(std.deriving.api.Deriving, std.core.Array<std.deriving.api.QualifiedName>)>, std.core.Array<std.deriving.api.QualifiedName>) { /* compiled code */ }

func interfacesBySimpleName(simpleName: std.core.String): std.core.Array<std.deriving.api.QualifiedName> { /* compiled code */ }

struct Registration <: std.core.Equatable<std.deriving.api.Registration> & std.core.Hashable {
    let id: Int64

    static func register(name: std.deriving.api.QualifiedName, candidate: std.deriving.api.Registration): Unit { /* compiled code */ }

    static func find(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Registration> { /* compiled code */ }

    static func find(simpleName: std.core.String): std.core.Array<std.deriving.api.Registration> { /* compiled code */ }

    override operator func ==(other: std.deriving.api.Registration): Bool { /* compiled code */ }

    override operator func !=(other: std.deriving.api.Registration): Bool { /* compiled code */ }

    override func hashCode(): Int64 { /* compiled code */ }

    let deriving: std.deriving.api.Deriving

    Registration(deriving: std.deriving.api.Deriving) { /* compiled code */ }
}
