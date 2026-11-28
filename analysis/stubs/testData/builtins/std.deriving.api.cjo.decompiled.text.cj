// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.api

import std.ast.*
import std.collection.*
import std.core.*
import std.collection.ArrayList
import std.unicode.*
import std.sync.AtomicInt64

public interface Logger {
    public open func warning(at: std.ast.Tokens, message: std.core.String): Unit { /* compiled code */ }

    public open func warning(at: std.ast.Token, message: std.core.String): Unit { /* compiled code */ }

    public open func error(at: std.ast.Tokens, message: std.core.String): Unit { /* compiled code */ }

    public open func error(at: std.ast.Token, message: std.core.String): Unit { /* compiled code */ }

    public open func log(at: std.ast.Tokens, message: std.core.String, error!: Bool): Unit
}

public interface DeclExt {
    public open mut prop modifiers: std.collection.ArrayList<std.ast.Modifier>

    public open func hasModifier(kind: std.ast.TokenKind): Bool { /* compiled code */ }

    public open func hasModifier(predicate: (std.ast.TokenKind) -> Bool): Bool { /* compiled code */ }

    public open func isStatic(): Bool { /* compiled code */ }

    public open func isPrivate(): Bool { /* compiled code */ }

    public open func unwrap(): std.ast.Decl
}

public struct QualifiedName <: std.core.ToString & std.core.Equatable<std.deriving.api.QualifiedName> & std.core.Hashable {
    init(tokens: std.core.Array<std.ast.Token>) { /* compiled code */ }

    init(name: std.core.String) { /* compiled code */ }

    init(qType: std.ast.QualifiedType) { /* compiled code */ }

    init(simpleName: std.deriving.api.Identifier) { /* compiled code */ }

    internal static func createUnchecked(tokens: std.core.Array<std.ast.Token>): std.deriving.api.QualifiedName { /* compiled code */ }

    public prop simpleName: std.ast.Token {
        get() { /* compiled code */ }
    }

    public func toSimpleNameQN(): std.deriving.api.QualifiedName { /* compiled code */ }

    public prop components: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    public operator func +(other: std.deriving.api.QualifiedName): std.deriving.api.QualifiedName { /* compiled code */ }

    public operator func +(other: std.core.String): std.deriving.api.QualifiedName { /* compiled code */ }

    public operator func +(other: std.ast.Token): std.deriving.api.QualifiedName { /* compiled code */ }

    public operator func +(other: std.deriving.api.Identifier): std.deriving.api.QualifiedName { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    public override operator func ==(other: std.deriving.api.QualifiedName): Bool { /* compiled code */ }

    public override operator func !=(other: std.deriving.api.QualifiedName): Bool { /* compiled code */ }

    public override func hashCode(): Int64 { /* compiled code */ }

    public let name: std.core.String

    public let tokens: std.core.Array<std.ast.Token>
}

public struct GenericDecl <: std.core.Equatable<std.deriving.api.GenericDecl> & std.core.Hashable & std.core.ToString {
    init(identifier: std.ast.Token) { /* compiled code */ }

    public override operator func ==(other: std.deriving.api.GenericDecl): Bool { /* compiled code */ }

    public override operator func !=(other: std.deriving.api.GenericDecl): Bool { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }

    public override func hashCode(): Int64 { /* compiled code */ }

    public let identifier: std.deriving.api.Identifier

    GenericDecl(identifier: std.deriving.api.Identifier) { /* compiled code */ }
}

public struct DerivingTarget {
    public prop interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings> {
        get() { /* compiled code */ }
    }

    public prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public func findIntefaceSettings(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.DerivingInterfaceSettings> { /* compiled code */ }

    public let declaration: std.ast.Decl

    public let namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    public let enumCases: std.core.Array<std.deriving.api.TargetEnumCase>

    public let settings: std.deriving.api.DerivingSettings

    DerivingTarget(declaration: std.ast.Decl, namedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, enumCases: std.core.Array<std.deriving.api.TargetEnumCase>, settings: std.deriving.api.DerivingSettings) { /* compiled code */ }
}

public interface GenericsInjector {
    public open func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, target: std.deriving.api.DerivingTarget): std.ast.TypeNode

    public open func constraintsFor(iface: std.deriving.api.QualifiedName, generic: std.deriving.api.GenericDecl, target: std.deriving.api.DerivingTarget): std.core.Array<std.ast.GenericConstraint> { /* compiled code */ }

    public static func noGenerics(): std.deriving.api.GenericsInjector { /* compiled code */ }

    public static func singleRecursive(): std.deriving.api.GenericsInjector { /* compiled code */ }
}

internal struct DefaultGenreicInjector <: std.deriving.api.GenericsInjector {
    public func injectGenerics(input: std.deriving.api.QualifiedName, _: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode { /* compiled code */ }

    init() { /* compiled code */ }
}

internal struct RecursiveGenericInjector <: std.deriving.api.GenericsInjector {
    public func injectGenerics(iface: std.deriving.api.QualifiedName, constrainedType: std.ast.TypeNode, _: std.deriving.api.DerivingTarget): std.ast.TypeNode { /* compiled code */ }

    init() { /* compiled code */ }
}

public struct DerivingSettings {
    public prop interfaceNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public let interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>

    public let excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>

    public let casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>

    public let fieldsOrder: std.core.Array<std.deriving.api.Identifier>

    public let includedProperties: std.core.Array<std.ast.PropDecl>

    DerivingSettings(interfaces: std.core.Array<std.deriving.api.DerivingInterfaceSettings>, excludedFields: std.core.Array<std.deriving.api.TargetNamedAttribute>, casesOrder: std.core.Array<std.deriving.api.TargetEnumCase>, fieldsOrder: std.core.Array<std.deriving.api.Identifier>, includedProperties: std.core.Array<std.ast.PropDecl>) { /* compiled code */ }
}

public struct DerivingInterfaceSettings {
    public let name: std.deriving.api.QualifiedName

    public let generics: std.core.Array<std.deriving.api.DerivingGenericSettings>

    DerivingInterfaceSettings(name: std.deriving.api.QualifiedName, generics: std.core.Array<std.deriving.api.DerivingGenericSettings>) { /* compiled code */ }
}

public enum TargetNamedAttribute {
    | FieldAttribute(std.ast.VarDecl)

    | ConstructorFieldAttribute(std.ast.FuncParam)

    | PropertyAttribute(std.ast.PropDecl)
}

public struct TargetEnumCase {
    public let name: std.core.String

    public let constructor: std.ast.Constructor

    public let fields: std.core.Array<std.deriving.api.TargetEnumField>

    TargetEnumCase(name: std.core.String, constructor: std.ast.Constructor, fields: std.core.Array<std.deriving.api.TargetEnumField>) { /* compiled code */ }
}

public struct Identifier <: std.core.ToString & std.core.Equatable<std.deriving.api.Identifier> & std.core.Hashable & std.ast.ToTokens {
    init(str: std.core.String) { /* compiled code */ }

    public prop value: std.core.String {
        get() { /* compiled code */ }
    }

    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(other: std.deriving.api.Identifier): Bool { /* compiled code */ }

    public operator func !=(other: std.deriving.api.Identifier): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public let token: std.ast.Token

    Identifier(token: std.ast.Token) { /* compiled code */ }
}

public struct TargetEnumField {
    public let position: Int64

    public let fieldType: std.ast.TypeNode

    TargetEnumField(position: Int64, fieldType: std.ast.TypeNode) { /* compiled code */ }
}

public struct DerivingGenericSettings <: std.core.ToString & std.ast.ToTokens {
    public prop hasUpperBounds: Bool {
        get() { /* compiled code */ }
    }

    public func toNode(): std.ast.GenericConstraint { /* compiled code */ }

    public func toTokens(): std.ast.Tokens { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public let generic: std.deriving.api.GenericDecl

    public let upperBounds: std.core.Array<std.ast.TypeNode>

    DerivingGenericSettings(generic: std.deriving.api.GenericDecl, upperBounds: std.core.Array<std.ast.TypeNode>) { /* compiled code */ }
}

public interface DerivingNode <: std.deriving.api.AstExt {
    public open func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit

    public open func appendAllConstraintsTo(deriving: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit
}

public interface Deriving {
    public open prop name: std.core.String

    public open prop description: std.core.String

    public open prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    public open func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    public open func derive(input: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens
}

public struct DerivingInterfaceInfo {
    public let name: std.deriving.api.QualifiedName

    public let superTypes: std.core.Array<std.deriving.api.QualifiedName>

    public let genericsInjector: std.deriving.api.GenericsInjector

    DerivingInterfaceInfo(name: std.deriving.api.QualifiedName, superTypes: std.core.Array<std.deriving.api.QualifiedName>, genericsInjector: std.deriving.api.GenericsInjector) { /* compiled code */ }
}

public interface AstExt {
    public open func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit

    public open func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit

    public open func appendFunction(f: std.ast.FuncDecl): Unit

    public open func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl
}

extend std.deriving.api.DerivingTarget {
    public func createExtendSkeleton(): std.ast.ExtendDecl { /* compiled code */ }

    internal func appendAllConstraintsTo(ext: std.ast.ExtendDecl, deriving: std.deriving.api.Deriving, iface: std.deriving.api.QualifiedName): Unit { /* compiled code */ }

    internal func appendAllConstraintsTo(ext: std.ast.ExtendDecl, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit { /* compiled code */ }

    public func upperBoundForExtend(iface: std.deriving.api.DerivingInterfaceInfo): std.ast.TypeNode { /* compiled code */ }

    public func genericConstraints(ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): std.core.Iterable<std.ast.GenericConstraint> { /* compiled code */ }
}

extend std.ast.ExtendDecl <: DerivingNode {
    public func appendAllConstraints(deriving: std.deriving.api.Deriving, target: std.deriving.api.DerivingTarget, iface: std.deriving.api.QualifiedName): Unit { /* compiled code */ }

    public func appendAllConstraintsTo(target: std.deriving.api.DerivingTarget, ifaceInfo: std.deriving.api.DerivingInterfaceInfo, settings: std.core.Option<std.deriving.api.DerivingInterfaceSettings>): Unit { /* compiled code */ }
}

extend std.deriving.api.DerivingTarget {
    public prop isNonFinal: Bool {
        get() { /* compiled code */ }
    }

    public prop isFinal: Bool {
        get() { /* compiled code */ }
    }

    public prop isSealedClass: Bool {
        get() { /* compiled code */ }
    }

    public prop isAbstractClass: Bool {
        get() { /* compiled code */ }
    }

    public prop allGenerics: std.core.Array<std.deriving.api.GenericDecl> {
        get() { /* compiled code */ }
    }

    public prop hasAnyGenerics: Bool {
        get() { /* compiled code */ }
    }

    public prop superTypes: std.core.Array<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }

    public prop superTypeNames: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public func toType(): std.ast.TypeNode { /* compiled code */ }

    internal func ifClass(predicate: (std.ast.ClassDecl) -> Bool): Bool { /* compiled code */ }

    internal func generics(): std.core.Option<std.ast.GenericParam> { /* compiled code */ }
}

extend std.deriving.api.TargetNamedAttribute {
    public prop decl: std.ast.Decl {
        get() { /* compiled code */ }
    }

    public prop identifier: std.deriving.api.Identifier {
        get() { /* compiled code */ }
    }

    public prop typeNode: std.core.Option<std.ast.TypeNode> {
        get() { /* compiled code */ }
    }
}

extend std.deriving.api.TargetNamedAttribute <: ToString {
    public func toString(): std.core.String { /* compiled code */ }
}

extend std.deriving.api.TargetEnumCase {
    public prop totalArgumentsCount: Int64 {
        get() { /* compiled code */ }
    }

    public prop components: std.core.Array<std.core.Option<std.deriving.api.TargetEnumField>> {
        get() { /* compiled code */ }
    }
}

extend std.deriving.api.GenericDecl {
    public func toType(): std.ast.RefType { /* compiled code */ }
}

extend std.deriving.api.Identifier {
    public func toType(): std.ast.RefType { /* compiled code */ }
}

public class CollectingLogger <: std.core.Object & std.deriving.api.Logger {
    private let elements: std.collection.ArrayList<std.core.String>

    public override func log(at: std.ast.Tokens, message: std.core.String, error!: Bool): Unit { /* compiled code */ }

    public func collect(): std.core.Array<std.core.String> { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal interface AppendGenerics {
    public open func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit
}

extend std.ast.RefType <: AppendGenerics {
    public func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit { /* compiled code */ }
}

extend std.ast.QualifiedType <: AppendGenerics {
    public func appendGenerics(args: std.core.Array<std.ast.TypeNode>): Unit { /* compiled code */ }
}

public interface BodyExt {
    public open func declsUnwrapped(): std.core.Iterator<std.ast.Decl>
}

public interface FuncParamExt {
    public open func unwrapParam(): std.ast.FuncParam
}

public interface FunctionExt {
    public open func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam>
}

extend std.ast.Decl <: DeclExt {
    public func unwrap(): std.ast.Decl { /* compiled code */ }
}

extend std.ast.Body <: BodyExt {
    public func declsUnwrapped(): std.core.Iterator<std.ast.Decl> { /* compiled code */ }
}

extend std.ast.FuncParam <: FuncParamExt {
    public func unwrapParam(): std.ast.FuncParam { /* compiled code */ }
}

extend std.ast.FuncDecl <: FunctionExt {
    public func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam> { /* compiled code */ }
}

extend std.ast.PrimaryCtorDecl <: FunctionExt {
    public func funcParamsUnwrapped(): std.core.Iterator<std.ast.FuncParam> { /* compiled code */ }
}

extend std.ast.ExtendDecl <: AstExt {
    public func appendFunction(f: std.ast.FuncDecl): Unit { /* compiled code */ }

    public func appendFunction(q: std.ast.Tokens): std.ast.FuncDecl { /* compiled code */ }

    public func appendSuperTypes(types: std.core.Iterable<std.ast.TypeNode>): Unit { /* compiled code */ }

    public func appendConstraints(constraints: std.core.Iterable<std.ast.GenericConstraint>): Unit { /* compiled code */ }
}

public func joinTokens(prefix!: std.ast.ToTokens = COMPILED_CODE /* compiled code */, postfix!: std.ast.ToTokens = COMPILED_CODE /* compiled code */, delimiter!: std.ast.ToTokens = COMPILED_CODE /* compiled code */, ifEmpty!: std.ast.ToTokens = COMPILED_CODE /* compiled code */): std.ast.Tokens { /* compiled code */ }

extend std.deriving.api.QualifiedName {
    public func toType(genericArgs: std.core.Array<std.ast.TypeNode>): std.ast.TypeNode { /* compiled code */ }
}

public func registerDeriving(implementation: std.deriving.api.Deriving): Unit { /* compiled code */ }

public func findDeriving(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Deriving> { /* compiled code */ }

public func findDerivings(names: std.core.Iterable<std.deriving.api.QualifiedName>): (std.core.Array<(std.deriving.api.Deriving, std.core.Array<std.deriving.api.QualifiedName>)>, std.core.Array<std.deriving.api.QualifiedName>) { /* compiled code */ }

public func interfacesBySimpleName(simpleName: std.core.String): std.core.Array<std.deriving.api.QualifiedName> { /* compiled code */ }

internal struct Registration <: std.core.Equatable<std.deriving.api.Registration> & std.core.Hashable {
    private let id: Int64

    internal static func register(name: std.deriving.api.QualifiedName, candidate: std.deriving.api.Registration): Unit { /* compiled code */ }

    internal static func find(name: std.deriving.api.QualifiedName): std.core.Option<std.deriving.api.Registration> { /* compiled code */ }

    internal static func find(simpleName: std.core.String): std.core.Array<std.deriving.api.Registration> { /* compiled code */ }

    public override operator func ==(other: std.deriving.api.Registration): Bool { /* compiled code */ }

    public override operator func !=(other: std.deriving.api.Registration): Bool { /* compiled code */ }

    public override func hashCode(): Int64 { /* compiled code */ }

    internal let deriving: std.deriving.api.Deriving

    Registration(deriving: std.deriving.api.Deriving) { /* compiled code */ }
}
