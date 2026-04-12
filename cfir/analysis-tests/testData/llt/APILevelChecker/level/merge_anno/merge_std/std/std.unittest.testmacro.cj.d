// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

macro package std.unittest.testmacro
import std.ast.*
import std.collection.*
import std.collection.concurrent.ConcurrentHashMap
import std.sync.AtomicUInt64

import std.unicode.{
    UnicodeRuneExtension,
    UnicodeStringExtension
}
import std.unittest.common.shieldEscapeSeq
import std.ast
import std.unicode.*
import std.core.*
import std.collection.map

/**
*
* The macro only add inner macro @Attribute to outer macro Test.
* It can use for-in expressionn to make Multiple Invocations.
* The macro can be only defined at func.
* @since 0.17.3
* @see Test
*/
public macro Expect(input: Tokens): Tokens

public macro Assert(input: Tokens): Tokens

public macro Expect(assertion: Tokens, body: Tokens): Tokens

public macro Assert(assertion: Tokens, body: Tokens): Tokens

/**
* This macro marks top-level function which is used as custom assertion in @Assert and @Expect macroses
*
* FIRST function argument should be of type AssertionCtx
*/
public macro CustomAssertion(body: Tokens)

public macro Bench(input: Tokens): Tokens

/**
*
* The macro @Bench can only be used on a  `func` declared with the @Testcase macro inside a test class.
* The `times` parameter in macro @Bench[times] can be used to specify the minimal benchmark iteration count, and
* `times` muse be non-negative integer.
* @since 0.17.3
* @see Test
*
* @throws MacroException if attr is not digital or input is not FuncDecl.
*/
public macro Bench(attr: Tokens, input: Tokens): Tokens

/**
* @Configure macro sets up configuration parameters for function or class
* Usage: @Configure[param1: value1, param2: value2, param3: value3]
* Each parameter name is a name of option defined in `@UnittestOption` macro or in unittest internals (case-sensitive).
* Each value is any Cangjie expression supported by astlib.
* In case if parameter is unknown (not registered via `@UnittestOption` or not defined in unittest internals)
* the error about undeclared identifier will be thrown.
* In case if type is invalid (e.g. not specified in `@UnittestOption`) the error about unsolvable types will be prined.
* For a particular list of configuration options supported by different parts of the framework, please refer to those parts.
* Macro can be repeated, in case of paramter duplication, up to type of value, last one will be selected.
*/
public macro Configure(args: Tokens, input: Tokens)

/*
* The macro immediately fails the test.
*/
public macro Fail(message: Tokens): Tokens

/*
* The macro NOT immediately fails the test.
*/
public macro FailExpect(message: Tokens): Tokens

public macro BeforeAll(input: Tokens)

public macro AfterAll(input: Tokens)

public macro BeforeEach(input: Tokens)

public macro AfterEach(input: Tokens)

/**
*
* The macro @Measure can only be used on a `class` or `top-level func` declared with the @Test macro.
* Attributes format: @Measure[<Measurement1>, <Measurement2>, ...]. 
* Accepted attributes: comma-separated non-empty list of Measurement instances.
* @see Test
* @see Bench
*
* @throws MacroException if attr is empty.
*/
public macro Measure(attr: Tokens, input: Tokens): Tokens

/**
* This macro can be defined at test class and means that:
* - All test cases are independent and can be run in parallel in separate processes.
* - beaforeAll() initialization and afterAll() cleanup are reentrant and can be run in parallel in separate processes.
*
* @since 0.17.3
* @see Test
* @see TestCase
* @see TestCases
*
* @throws MacroException if input is not ClassDecl or FuncDecl.
*/
public macro Parallel(input: Tokens): Tokens

public macro PowerAssert(input: Tokens): Tokens

/**
*
* The macro only add inner macro @Attribute to outer macro Test.
* It can skip the func to execute.
* @since 0.17.3
* @see Test
*
* @throws MacroException if input is not ClassDecl or FuncDecl.
*/
public macro Skip(input: Tokens): Tokens

/**
*
* The macro only add inner macro @Attribute to outer macro Test.
* It can skip the func to execute.
* If attr is the true identifier, it is same with {@codeSkip()}.
* If attr is other tokens, it is Ineffective.
* @since 0.17.3
* @see Test
*
* @throws MacroException if input is not ClassDecl or FuncDecl.
*/
public macro Skip(attr: Tokens, input: Tokens): Tokens

public macro Strategy(input: Tokens): Tokens

/**
* This Macro is used to combine and map existing strategies into a new strategy.
* Can be used independently from @Test macro.
*
* @throws MacroException if input is not FuncDecl.
* @throws MacroException if function return type is not specified.
*/
public macro Strategy(attr: Tokens, input: Tokens): Tokens

public macro Tag(tags: Tokens, input: Tokens): Tokens

public macro TestBuilder(input: Tokens): Tokens

/**
*
* The macro only add inner macro @Attribute to outer macro Test.
* The macro can be only defined at func.
*
* @since 0.17.3
* @see Test
*
* @throws MacroException if input is not FuncDecl.
*/
public macro TestCase(input: Tokens): Tokens

public macro TestCase(args: Tokens, input: Tokens): Tokens

/**
* The macro can be only defined at class or top-level func.
* The macro expand to TestCase and Parses attributes passed by  macros.
* It will delegate CasesFilterChain to integrate them.
*
* @since 0.17.3
* @see Test
* @see TestCase
* @see CasesFilterChain
*
* @throws MacroException if input is not ClassDecl or FuncDecl.
* @throws IllegalArgumentException if input is ClassDecl and found PrimaryCtorDecl in class body,
* and the block of PrimaryCtorDecl body returns a None value.
*/
public macro Test(input: Tokens): Tokens

public macro Test(dsl: Tokens, input: Tokens): Tokens

public macro TestTemplate(input: Tokens): Tokens

public macro TestTemplate(dsl: Tokens, input: Tokens): Tokens

/*
* Immediately fails the test if Exception instance was not thrown.
*/
public macro AssertThrows(body: Tokens): Tokens

/*
* Immediately fails the test if one of 'exceptions' was not thrown.
*/
public macro AssertThrows(exceptions: Tokens, body: Tokens): Tokens

/*
* Fails the test if Exception instance was not thrown.
*/
public macro ExpectThrows(body: Tokens): Tokens

/*
* Fails the test if one of 'exceptions' was not thrown.
*/
public macro ExpectThrows(exceptions: Tokens, body: Tokens): Tokens

/**
* This macro can be defined at test case and enables timeout for it.
* This macro can be defined at test class and enables timeout for every corresponding test case by default.
* Timeout specified for the test case overlaps timeout specified for enclosing test class.
*
* This macro has a parameter that should be an expresion of type std.core.Duration.
* If test case do not finish in the specified time, it is terminated.
*
* @since 0.17.3
* @see Test
* @see TestCase
* @see TestCases
* @see Duration
*
* @throws MacroException if input is not ClassDecl or FuncDecl.
*/
public macro Timeout(attrs: Tokens, input: Tokens): Tokens

/**
* Provide types for a generic test class or test function.
* Parse parameters in the form @Types[T1 in <A, B, C>, T2 in <D, E>, ...]
* where T1, T2 ... are generic type parameters of the annotated declaration, and A, B, C, D, E... are types.
* The corresponding test will be run on each combination of all provided parameter types.
* This macro must be used together with @Test, @TestCase or @Bench macro.
* This macro is not repeatable, only one must be used for a single declaration.
* The type argument specified using this macro must be the same as the generic type parameters of the annotated declaration.
*/
public macro Types(dsl: Tokens, input: Tokens)

public macro UnittestOption(_: Tokens): Tokens

public macro UnittestOption(types: Tokens, params: Tokens): Tokens

