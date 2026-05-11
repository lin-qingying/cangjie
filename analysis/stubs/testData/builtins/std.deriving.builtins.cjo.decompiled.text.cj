// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.builtins

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.resolve.Types

class ArbitraryDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

func commaSeparated(things: std.core.Iterable<T>): std.ast.Tokens { /* compiled code */ }

class ComparableDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

class EquatableDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

class HashableDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

class ShrinkDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

class ToStringDeriving <: std.core.Object & std.deriving.api.Deriving {
    override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

func destructEnumCase(enumCase: std.deriving.api.TargetEnumCase, prefix: std.core.String): std.ast.Tokens { /* compiled code */ }

func enumFallback(target: std.deriving.api.DerivingTarget): std.ast.Tokens { /* compiled code */ }

func isEnumHasEllipsis(target: std.deriving.api.DerivingTarget): Bool { /* compiled code */ }
