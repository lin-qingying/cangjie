// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.argopt

import std.collection.*
import std.core.*

class ArgOpt {
    var utils: std.argopt.ParserHelper

    var arguments: std.argopt.Arguments

    init(shortArgFormat: std.core.String) { /* compiled code */ }

    init(longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    init(shortArgFormat: std.core.String, longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    init(args: std.core.Array<std.core.String>, shortArgFormat: std.core.String, longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    func getArg(arg: std.core.String): std.core.Option<std.core.String> { /* compiled code */ }

    func getUnparseArgs(): std.core.Array<std.core.String> { /* compiled code */ }

    func getArgumentsMap(): std.collection.HashMap<std.core.String, std.core.String> { /* compiled code */ }
}

class ParserHelper {
    let argMap: std.collection.HashMap<std.core.String, std.core.String>

    var argsList: std.collection.ArrayList<Int8>

    var arguments: std.argopt.Arguments
}

class Arguments {
    var myArgs: std.core.Array<std.core.String>

    var unparseArgs: std.core.Array<std.core.String>

    init() { /* compiled code */ }
}

enum ArgumentMode <: std.core.ToString & std.core.Equatable<std.argopt.ArgumentMode> {
    | NoValue

    | RequiredValue

    | OptionalValue

    func toString(): std.core.String { /* compiled code */ }

    operator func ==(that: std.argopt.ArgumentMode): Bool { /* compiled code */ }
}

enum ArgumentSpec {
    | Short(Rune, std.argopt.ArgumentMode)

    | Short(Rune, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | Long(std.core.String, std.argopt.ArgumentMode)

    | Long(std.core.String, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | Full(std.core.String, Rune, std.argopt.ArgumentMode)

    | Full(std.core.String, Rune, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | NonOptions((std.core.Array<std.core.String>) -> Unit)
}

struct ParsedArguments {
    var _options: std.collection.HashMap<std.core.String, std.core.String>

    var _nonOptions: std.core.Array<std.core.String>

    prop options: std.collection.ReadOnlyMap<std.core.String, std.core.String> {
        get() { /* compiled code */ }
    }

    prop nonOptions: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }
}

class ArgumentParseException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

func parseArguments(args: std.core.Array<std.core.String>, specs: std.core.Array<std.argopt.ArgumentSpec>): std.argopt.ParsedArguments { /* compiled code */ }
