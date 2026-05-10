// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.builtins

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.resolve.Types

class ArbitraryDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

func commaSeparated(things: std.core.Iterable<T>): std.ast.Tokens

class ComparableDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

class EquatableDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

class HashableDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

class ShrinkDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

class ToStringDeriving <: std.core.Object & std.deriving.api.Deriving {
    prop name: std.core.String

    prop description: std.core.String

    prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName>

    func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo

    func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens

    init() { /* compiled code */ }
}

func destructEnumCase(enumCase: std.deriving.api.TargetEnumCase, prefix: std.core.String): std.ast.Tokens

func enumFallback(target: std.deriving.api.DerivingTarget): std.ast.Tokens

func isEnumHasEllipsis(target: std.deriving.api.DerivingTarget): Bool
