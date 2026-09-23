
package std.argopt
import std.collection.*


/**
 * @description A command-line argument parsing class. Use the global parseArguments function instead for new code.
 */
@!APILevel[
    since: "22"
]
public class ArgOpt {
    /**
     * @description Constructor with short argument format specification
     * @param shortArgFormat - Short parameter format string where characters followed by ':' expect values (e.g., "abc:d:" means 'c' and 'd' expect values)
     */
    @!APILevel[
        since: "22"
    ]
    public init(shortArgFormat: String)
    
    /**
     * @description Constructor with long argument specification
     * @param longArgList - Array of long parameter names where names ending with '=' expect values (e.g., ["testA=", "testB"] means testA expects a value)
     */
    @!APILevel[
        since: "22"
    ]
    public init(longArgList: Array<String>)
    
    /**
     * @description Constructor with both short and long argument specifications
     * @param shortArgFormat - Short parameter format string
     * @param longArgList - Array of long parameter names
     */
    @!APILevel[
        since: "22"
    ]
    public init(shortArgFormat: String, longArgList: Array<String>)
    
    /**
     * @description Main constructor that parses arguments immediately using both short and long format specifications
     * @param args - Command-line arguments to parse
     * @param shortArgFormat - Short parameter format string
     * @param longArgList - Array of long parameter names
     */
    @!APILevel[
        since: "22"
    ]
    public init(args: Array<String>, shortArgFormat: String, longArgList: Array<String>)
    
    /**
     * @description Retrieves the value of a parsed argument
     * @param arg - Argument name (with or without '-' prefix)
     * @returns Option containing the argument value if found, None otherwise
     */
    @!APILevel[
        since: "22"
    ]
    public func getArg(arg: String): Option<String>
    
    /**
     * @description Returns arguments that were not parsed as options
     * @returns Array of unparsed argument strings
     */
    @!APILevel[
        since: "22"
    ]
    public func getUnparseArgs(): Array<String>
    
    /**
     * @description Returns all parsed arguments as key-value pairs
     * @returns HashMap with argument names as keys and their values as values
     */
    @!APILevel[
        since: "22"
    ]
    public func getArgumentsMap(): HashMap<String, String>
}

/**
 * @description Enumeration specifying whether command-line arguments require values
 */
@!APILevel[
    since: "22"
]
public enum ArgumentMode <: ToString & Equatable<ArgumentMode> {
    /**
     * @description Option requires no value
     */
    @!APILevel[
        since: "22"
    ]
    NoValue |
    /**
     * @description Option requires a value
     */
    @!APILevel[
        since: "22"
    ]
    RequiredValue |
    /**
     * @description Option accepts an optional value
     */
    @!APILevel[
        since: "22"
    ]
    OptionalValue
    /**
     * @description Converts the ArgumentMode to its string representation
     * @returns String representation of the mode
     */
    @!APILevel[
        since: "22"
    ]
    public func toString(): String
    
    /**
     * @description Compares this ArgumentMode with another for equality
     * @param other - The ArgumentMode to compare with
     * @returns True if both modes are equal, false otherwise
     */
    @!APILevel[
        since: "22"
    ]
    public operator func ==(other: ArgumentMode): Bool
}

/**
 * @description Specification for different types of command-line arguments including short options, long options, and their combinations
 */
@!APILevel[
    since: "22"
]
public enum ArgumentSpec {
    /**
     * @description Short option specification with a single character and argument mode
     */
    @!APILevel[
        since: "22"
    ]
    Short(Rune, ArgumentMode) |
    /**
     * @description Short option specification with callback action
     */
    @!APILevel[
        since: "22"
    ]
    Short(Rune, ArgumentMode, (String) -> Unit) |
    /**
     * @description Long option specification with name and argument mode
     */
    @!APILevel[
        since: "22"
    ]
    Long(String, ArgumentMode) |
    /**
     * @description Long option specification with callback action
     */
    @!APILevel[
        since: "22"
    ]
    Long(String, ArgumentMode, (String) -> Unit) |
    /**
     * @description Full option specification with both long name and short character
     */
    @!APILevel[
        since: "22"
    ]
    Full(String, Rune, ArgumentMode) |
    /**
     * @description Full option specification with callback action
     */
    @!APILevel[
        since: "22"
    ]
    Full(String, Rune, ArgumentMode, (String) -> Unit) |
    /**
     * @description Specification for handling non-option arguments with a callback
     */
    @!APILevel[
        since: "22"
    ]
    NonOptions((Array<String>) -> Unit)
}

/**
 * @description Structure containing the results of argument parsing
 */
@!APILevel[
    since: "22"
]
public struct ParsedArguments {
    /**
     * @description Map of parsed options with their names as keys and values as values
     */
    @!APILevel[
        since: "22"
    ]
    public prop options: ReadOnlyMap<String, String>
    
    /**
     * @description Array of non-option arguments (positional arguments)
     */
    @!APILevel[
        since: "22"
    ]
    public prop nonOptions: Array<String>
}

/**
 * @description Exception thrown when argument parsing fails
 */
@!APILevel[
    since: "22"
]
public class ArgumentParseException <: Exception {
    /**
     * @description Default constructor with no message
     */
    @!APILevel[
        since: "22"
    ]
    public init()
    
    /**
     * @description Constructor with error message
     * @param message - Error message describing the parsing failure
     */
    @!APILevel[
        since: "22"
    ]
    public init(message: String)
}

/**
 * @description Parses command-line arguments according to the provided specifications
 * @param args - Array of command-line argument strings to parse
 * @param specs - Array of argument specifications defining expected options
 * @returns ParsedArguments containing parsed options and non-option arguments
 * @throws ArgumentParseException when parsing fails due to invalid arguments or missing required values
 * @throws IllegalArgumentException when defining ArgumentSpec with the same name
 */
@!APILevel[
    since: "22",
    throwexception: true
]
public func parseArguments(args: Array<String>, specs: Array<ArgumentSpec>): ParsedArguments

