// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.



package std.argopt
import std.collection.*


/**
* This is a library for ArgOpt class.
* This module helps scripts parse command-line arguments,
* And parse functions (including special meanings of parameters.
* in the form of "-" and "--") , Supported by the GNU software.
* This class uses some methods of String and Rune classes for implementation.
*
* @since 0.17.4
*/
@Deprecated[message: "Use global function `public func parseArguments(args: Array<String>, specs: Array<ArgumentSpec>): ParsedArguments` instead."]
@!APILevel[since: "12", atomicservice : true]
public class ArgOpt {
    /**
    * Constructor one parameters.
    * For example : shortArgFormat ="abc:d:".
    * indicates that the first letter before the colon can be parsed. that's c and d.
    *
    * @see init(args: Array<String>, shortArgFormat: String, longArgList:Array<String>.
    * @param shortArgFormat Short parameter.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(shortArgFormat: String)
    
    /**
    * Constructor one parameters.
    * For example: longArgList = {"testA=", "testB"}, args = {"--testA=foo","--test=bar"}
    * Because of the r'=' exists after testA in longArgList. you can find the value of.
    * testA in args. Conversely, no value found.
    *
    * @see init(args: Array<String>, shortArgFormat: String, longArgList: Array<String>).
    * @param longArgList Long parameter set.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(longArgList: Array<String>)
    
    /**
    * Constructor two parameters.
    * For example, see the context.
    *
    * @see init(shortArgFormat: String).
    * @see init(longArgList: Array<String>).
    * @see init(args: Array<String>, shortArgFormat: String, longArgList: Array<String>).
    * @param longArgList Long parameter set.
    * @param shortArgFormat Short parameter.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(shortArgFormat: String, longArgList: Array<String>)
    
    /**
    * This constructor is parsed using the ParserHelper class.
    * which implements the overall round robin scheduling.
    * The split parameters are placed in ParserHelper.argMap.
    * The indexes that do not parse parameters are placed in ParserHelper.argsList.
    * For example No.1: shortArgFormat="abc:d:", args= {"-afoo", "-bofo", "-cbar", "-d"}.
    * Because indicates that the first letter before the colon can be parsed.
    * Keys '-a', '-b', '-c', and '-d' have been saved in the key of ParserHelper.argMap.
    * so the value of r'c' is "bar", the value of r'd' is "".
    * For example No.2: longArgList= {"testA=", "testB"}, args= {"--testA=foo", "--testB=bar"}.
    * Because of the r'=' exists after testA in longArgList.
    * you can find the value of testA in args. Conversely, no value found.
    * Keys 'testA' and 'testB' have been parsed into the key of ParserHelper.argMap.
    * However, The values of keys 'testA' and 'testB' depend on the parametric form of 'longArgList'.
    * For example No.3: longArgList= {"testA=", "testB"},args= {"--testA", "foo", "--testB", "bar"}.
    * Because of the r'=' exists after testA in longArgList.
    * But, if '--testA' in 'args' does not have a value (value after r'='), the next value of '--testA' must be obtained.
    * In addition, the prefix of this next value cannot be '--' or r'-'.
    * In the same way, Keys 'testA' and 'testB' have been parsed into the key of ParserHelper.argMap.
    *
    * @see Arguments.init().
    * @see Arguments.getDoubleSign().
    * @see Arguments.getSingleSign().
    * @see Arguments.setMyArgs(String).
    * @see ParserHelper.init(Arguments).
    * @see ParserHelper.returnTupleFun(String).
    * @see ParserHelper.isArrayEmptyFun(Array<String>).
    * @param longArgList Long parameter set.
    * @param shortArgFormat Short parameter.
    * @param args setting Default parameters of myArgs.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public init(args: Array<String>, shortArgFormat: String, longArgList: Array<String>)
    
    /**
    * Getting the value of a parameter.
    * For example one: getArg("a") or getArg("-a").
    * The prefix of the input parameter can be without r'-' or can contain r'-'.
    * which does not affect the returned value.
    * For example two: getArg("--test").
    *
    * @param arg Target key.
    * @return return the option type corresponding to the parameter.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public func getArg(arg: String): Option<String>
    
    /**
    * Getting the are no parsed parameters.
    *
    * @return if there are no unresolved parameters, returns Arguments.NULL.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public func getUnparseArgs(): Array<String>
    
    /**
    * Getting all parsed parameters and values.
    *
    * @return key-value pair. The header of the key value must contain r'-' or '--'.
    *
    * @since 0.17.4
    */
    @!APILevel[since: "12", atomicservice : true]
    public func getArgumentsMap(): HashMap<String, String>
}

@!APILevel[since: "12", atomicservice : true]
public enum ArgumentMode <: ToString & Equatable<ArgumentMode> {
    @!APILevel[since: "12", atomicservice : true]
    NoValue |
    @!APILevel[since: "12", atomicservice : true]
    RequiredValue |
    @!APILevel[since: "12", atomicservice : true]
    OptionalValue
    @!APILevel[since: "12", atomicservice : true]
    public func toString(): String
    
    @!APILevel[since: "12", atomicservice : true]
    public operator func ==(that: ArgumentMode): Bool
}

@!APILevel[since: "12", atomicservice : true]
public enum ArgumentSpec {
    @!APILevel[since: "12", atomicservice : true]
    Short(Rune, ArgumentMode) |
    @!APILevel[since: "12", atomicservice : true]
    Short(Rune, ArgumentMode, (String) -> Unit) |
    @!APILevel[since: "12", atomicservice : true]
    Long(String, ArgumentMode) |
    @!APILevel[since: "12", atomicservice : true]
    Long(String, ArgumentMode, (String) -> Unit) |
    @!APILevel[since: "12", atomicservice : true]
    Full(String, Rune, ArgumentMode) |
    @!APILevel[since: "12", atomicservice : true]
    Full(String, Rune, ArgumentMode, (String) -> Unit) |
    @!APILevel[since: "12", atomicservice : true]
    NonOptions((Array<String>) -> Unit)
}

@!APILevel[since: "12", atomicservice : true]
public struct ParsedArguments {
    @!APILevel[since: "12", atomicservice : true]
    public prop options: ReadOnlyMap<String, String>
    
    @!APILevel[since: "12", atomicservice : true]
    public prop nonOptions: Array<String>
}

@!APILevel[since: "12", atomicservice : true]
public class ArgumentParseException <: Exception {
    @!APILevel[since: "12", atomicservice : true]
    public init()
    
    @!APILevel[since: "12", atomicservice : true]
    public init(message: String)
}

@!APILevel[since: "12", atomicservice : true]
public func parseArguments(args: Array<String>, specs: Array<ArgumentSpec>): ParsedArguments

