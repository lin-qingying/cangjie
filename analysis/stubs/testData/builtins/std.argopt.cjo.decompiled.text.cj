// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.argopt

import std.collection.*
import std.core.*

@Deprecated[message: "Use global function `public func parseArguments(args: Array<String>, specs: Array<ArgumentSpec>): ParsedArguments` instead."]
@!APILevel[since: "22"]
public class ArgOpt {
    private var utils: std.argopt.ParserHelper

    private var arguments: std.argopt.Arguments

    @!APILevel[since: "22"]
    init(shortArgFormat: std.core.String) { /* compiled code */ }

    @!APILevel[since: "22"]
    init(longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    @!APILevel[since: "22"]
    init(shortArgFormat: std.core.String, longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    @!APILevel[since: "22"]
    init(args: std.core.Array<std.core.String>, shortArgFormat: std.core.String, longArgList: std.core.Array<std.core.String>) { /* compiled code */ }

    @!APILevel[since: "22"]
    public func getArg(arg: std.core.String): std.core.Option<std.core.String> { /* compiled code */ }

    @!APILevel[since: "22"]
    public func getUnparseArgs(): std.core.Array<std.core.String> { /* compiled code */ }

    @!APILevel[since: "22"]
    public func getArgumentsMap(): std.collection.HashMap<std.core.String, std.core.String> { /* compiled code */ }
}

internal class ParserHelper {
    private let argMap: std.collection.HashMap<std.core.String, std.core.String>

    private var argsList: std.collection.ArrayList<Int8>

    private var arguments: std.argopt.Arguments
}

internal class Arguments {
    private var myArgs: std.core.Array<std.core.String>

    private var unparseArgs: std.core.Array<std.core.String>

    init() { /* compiled code */ }
}

@!APILevel[since: "22"]
public enum ArgumentMode <: std.core.ToString & std.core.Equatable<std.argopt.ArgumentMode> {
    | @!APILevel[since: "22"]
    NoValue

    | @!APILevel[since: "22"]
    RequiredValue

    | @!APILevel[since: "22"]
    OptionalValue

    @!APILevel[since: "22"]
    public func toString(): std.core.String { /* compiled code */ }

    public operator func ==(that: std.argopt.ArgumentMode): Bool { /* compiled code */ }
}

@!APILevel[since: "22"]
public enum ArgumentSpec {
    | @!APILevel[since: "22"]
    Short(Rune, std.argopt.ArgumentMode)

    | @!APILevel[since: "22"]
    Short(Rune, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | @!APILevel[since: "22"]
    Long(std.core.String, std.argopt.ArgumentMode)

    | @!APILevel[since: "22"]
    Long(std.core.String, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | @!APILevel[since: "22"]
    Full(std.core.String, Rune, std.argopt.ArgumentMode)

    | @!APILevel[since: "22"]
    Full(std.core.String, Rune, std.argopt.ArgumentMode, (std.core.String) -> Unit)

    | @!APILevel[since: "22"]
    NonOptions((std.core.Array<std.core.String>) -> Unit)
}

@!APILevel[since: "22"]
public struct ParsedArguments {
    internal var _options: std.collection.HashMap<std.core.String, std.core.String>

    internal var _nonOptions: std.core.Array<std.core.String>

    @!APILevel[since: "22"]
    public prop options: std.collection.ReadOnlyMap<std.core.String, std.core.String> {
        get() { /* compiled code */ }
    }

    @!APILevel[since: "22"]
    public prop nonOptions: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }
}

@!APILevel[since: "22"]
public class ArgumentParseException <: std.core.Exception {
    @!APILevel[since: "22"]
    init() { /* compiled code */ }

    @!APILevel[since: "22"]
    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

@!APILevel[since: "22", throwexception: true]
public func parseArguments(args: std.core.Array<std.core.String>, specs: std.core.Array<std.argopt.ArgumentSpec>): std.argopt.ParsedArguments { /* compiled code */ }
