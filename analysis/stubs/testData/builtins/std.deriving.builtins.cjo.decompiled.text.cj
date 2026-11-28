// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.deriving.builtins

import std.deriving.api.*
import std.ast.*
import std.collection.*
import std.core.*
import std.deriving.resolve.Types

internal class ArbitraryDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func commaSeparated<T>(things: std.core.Iterable<T>): std.ast.Tokens { /* compiled code */ }

internal class ComparableDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class EquatableDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class HashableDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class ShrinkDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, logger: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class ToStringDeriving <: std.core.Object & std.deriving.api.Deriving {
    public override prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop description: std.core.String {
        get() { /* compiled code */ }
    }

    public override prop supportedInterfaces: std.core.Array<std.deriving.api.QualifiedName> {
        get() { /* compiled code */ }
    }

    public override func queryInterfaceInfo(interfaceName: std.deriving.api.QualifiedName): std.deriving.api.DerivingInterfaceInfo { /* compiled code */ }

    public override func derive(_: std.ast.Tokens, target: std.deriving.api.DerivingTarget, _: std.deriving.api.Logger): std.ast.Tokens { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func destructEnumCase(enumCase: std.deriving.api.TargetEnumCase, prefix: std.core.String): std.ast.Tokens { /* compiled code */ }

internal func enumFallback(target: std.deriving.api.DerivingTarget): std.ast.Tokens { /* compiled code */ }

internal func isEnumHasEllipsis(target: std.deriving.api.DerivingTarget): Bool { /* compiled code */ }
