// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.








package std.unittest
import std.collection.*

import std.unittest.diff.AssertPrintable
import std.unittest.common.*
import std.time.DateTime
import std.time.*
import std.math.*
import std.runtime.*
import std.process.*
import std.fs.*
import std.random.*
import std.convert.*
import std.sort.*
import std.sync.*
import std.argopt.*
import std.unicode.*
import std.random.Random
import std.unittest.prop_test.RandomSource
import std.unittest.common.KeyTags
import std.unittest.prop_test.KeyRandom
import std.collection.concurrent.ConcurrentLinkedQueue
import std.io.*
import std.unittest.mock.*
import std.unittest.prop_test.*
import std.unittest.common.optionsInfo
import std.regex.*
import std.env.*
import std.sync.AtomicOptionReference
import std.collection.{
    ArrayList,
    collectString
}
import std.sync.ReentrantMutex
public import std.unittest.common.Configuration
public import std.unittest.common.ConfigurationKey
public import std.unittest.common.DataProvider
public import std.unittest.common.DataStrategy
public import std.unittest.common.DataShrinker
public import std.unittest.common.registerOptionValidator
public import std.unittest.common.setOptionInfo
public import std.unittest.common.OptionValidity
public import std.unittest.common.KeyFor
public import std.unittest.common.KeyTags
public import std.unittest.prop_test.Arbitrary
public import std.unittest.prop_test.Shrink
public import std.unittest.prop_test.random
public import std.unittest.prop_test.KeyRandom
import std.unittest.mock.PrettyException
import std.convert.Formattable
import std.collection.concurrent.NonBlockingQueue
import std.env.atExit
import std.time.MonoTime
import std.collection.any
import std.collection.all
import std.collection.ArrayList
import std.collection.filter
import std.collection.filterMap
import std.collection.first
import std.collection.last
import std.collection.map
import std.collection.collectArray
import std.collection.collectString
import std.collection.HashMap
import std.collection.HashSet
import std.math.abs
import std.collection.concurrent.*
import std.collection.{
    HashMap,
    ArrayList
}
import std.sort.SortExtension
import std.unittest.common.DataStrategy
import std.unittest.prop_test.ShrinkHelpers
import std.unittest.common.toStringOrPlaceholder
import std.env.exit
import std.fs.{
    Path,
    File,
    exists
}
import std.env.getTempDirectory
import std.binary.*
import std.collection.zip
import std.io.IOException
import std.net.*
import std.core.println as corePrintln
import std.core.print as corePrint

@!APILevel[12, atomicservice : true]
public class TestSuite {
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public func runTests(): TestReport
    
    @!APILevel[12, atomicservice : true]
    public func runTests(configuration: Configuration): TestReport
    
    @!APILevel[12, atomicservice : true]
    public func runBenchmarks(): BenchReport
    
    @!APILevel[12, atomicservice : true]
    public func runBenchmarks(configuration: Configuration): BenchReport
    
    @!APILevel[12, atomicservice : true]
    public static func builder(name: String): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public static func builder(suite: TestSuite): TestSuiteBuilder
}

@!APILevel[12, atomicservice : true]
public class TestGroup {
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public func runTests(): TestReport
    
    @!APILevel[12, atomicservice : true]
    public func runTests(configuration: Configuration): TestReport
    
    @!APILevel[12, atomicservice : true]
    public func runBenchmarks(): BenchReport
    
    @!APILevel[12, atomicservice : true]
    public func runBenchmarks(configuration: Configuration): BenchReport
    
    @!APILevel[12, atomicservice : true]
    public static func builder(name: String): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public static func builder(group: TestGroup): TestGroupBuilder
}

@!APILevel[12, atomicservice : true]
public class TestGroupBuilder {
    @!APILevel[12, atomicservice : true]
    public func setName(name: String): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public func configure(configuration: Configuration): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public func add(test: UnitTestCase): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public func add(benchmark: Benchmark): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public func add(suite: TestSuite): TestGroupBuilder
    
    @!APILevel[12, atomicservice : true]
    public func build(): TestGroup
}

@!APILevel[12, atomicservice : true]
public class TestSuiteBuilder {
    @!APILevel[12, atomicservice : true]
    public func setName(name: String): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func configure(configuration: Configuration): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func beforeEach(body: (String) -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func afterEach(body: (String) -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func beforeEach(body: () -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func afterEach(body: () -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func beforeAll(body: () -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func afterAll(body: () -> Unit): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func add(test: UnitTestCase): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func add(benchmark: Benchmark): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func template(template: TestSuite): TestSuiteBuilder
    
    @!APILevel[12, atomicservice : true]
    public func build(): TestSuite
}

@!APILevel[12, atomicservice : true]
public class UnitTestCase {
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public func run(): TestReport
    
    @!APILevel[12, atomicservice : true]
    public static func createParameterized<T>(name: String, strategy: DataStrategy<T>, configuration!: Configuration = Configuration(), body!: (T) -> Unit): UnitTestCase
    
    @!APILevel[12, atomicservice : true]
    public static func createParameterized<T>(name: String, strategy: DataStrategyProcessor<T>, configuration!: Configuration = Configuration(), body!: (T) -> Unit): UnitTestCase
    
    @!APILevel[12, atomicservice : true]
    public static func create(name: String, configuration!: Configuration = Configuration(), body!: () -> Unit): UnitTestCase
}

@!APILevel[12, atomicservice : true]
public class Benchmark {
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public func run(): BenchReport
    
    // Creates parameterized benchmark with provided data strategy.// // Note: this api is inferior to @Bench macro because it uses lambda which can cause undesired overhead. // It should not be used for micro benchmarks.
    @!APILevel[12, atomicservice : true]
    public static func createParameterized<T>(
        name: String, 
        strategy: DataStrategy<T>, 
        configuration!: Configuration = Configuration(), 
        measurement!: Measurement = TimeNow(), 
        body!: (T) -> Unit
    ): Benchmark
    
    // Creates parameterized benchmark with provided data strategy processor.// // Note: this api is inferior to @Bench macro because it uses lambda which can cause undesired overhead. // It should not be used for micro benchmarks.
    @!APILevel[12, atomicservice : true]
    public static func createParameterized<T>(
        name: String, 
        strategy: DataStrategyProcessor<T>, 
        configuration!: Configuration = Configuration(), 
        measurement!: Measurement = TimeNow(), 
        body!: (T) -> Unit
    ): Benchmark
    
    // Creates parameterized benchmark from given lambda.// // Note: this api is inferior to @Bench macro because it uses lambda which can cause undesired overhead. // It should not be used for micro benchmarks.
    @!APILevel[12, atomicservice : true]
    public static func create(
        name: String, 
        configuration!: Configuration = Configuration(), 
        measurement!: Measurement = TimeNow(), 
        body!: () -> Unit
    ): Benchmark
}

@!APILevel[12, atomicservice : true]
sealed abstract class Report {
    @!APILevel[12, atomicservice : true]
    public prop errorCount: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop caseCount: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop passedCount: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop failedCount: Int64
    
    @!APILevel[12, atomicservice : true]
    public prop skippedCount: Int64
}

@!APILevel[12, atomicservice : true]
public class TestReport <: Report {
    @!APILevel[12, atomicservice : true]
    public func reportTo<T>(reporter: Reporter<TestReport, T>): T
}

@!APILevel[12, atomicservice : true]
public class BenchReport <: Report {
    @!APILevel[12, atomicservice : true]
    public func reportTo<T>(reporter: Reporter<BenchReport, T>): T
}

@!APILevel[12, atomicservice : true]
public interface TestClass {
}

// NOTE: this class is used internally by the compiler and is considered implementation details// TODO: rename to __TestRegister
@!APILevel[12, atomicservice : true]
public class TestPackage {
    @!APILevel[12, atomicservice : true]
    public TestPackage(
        let name: String
    )
    
    @!APILevel[12, atomicservice : true]
    public func registerCase(testCase: () -> UnitTestCase): Unit
    
    @!APILevel[12, atomicservice : true]
    public func registerSuite(suite: () -> TestSuite): Unit
    
    @!APILevel[12, atomicservice : true]
    public func registerBench(bench: () -> Benchmark): Unit
}

/**
* Checks two values for equality and will throw an exception if the check fails.
*
* @param leftStr string representation of expected.
* @param rightStr string representation of actual.
* @param expected expected value.
* @param actual actual value.
* @param optParentCtx context where fail message is stored

* @throws AssertException If <expected> and <actual> are not equal.
*/
@!APILevel[12, atomicservice : true]
public func assertEqual<T>(leftStr: String, rightStr: String, expected: T, actual: T, optParentCtx!: ?AssertionCtx = None): Unit where T <: Equatable<T>

/**
* Same as assertEqual but will not throw an exception immediately.
*
* @param leftStr string representation of expected.
* @param rightStr string representation of actual.
* @param expected expected value.
* @param actual actual value.
* @param optParentCtx context where fail message is stored
*/
@!APILevel[12, atomicservice : true]
public func expectEqual<T>(leftStr: String, rightStr: String, expected: T, actual: T, optParentCtx!: ?AssertionCtx = None): Unit where T <: Equatable<T>

/**
* The AssertException.
*
*/
@!APILevel[12, atomicservice : true]
public class AssertException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

/**
* The AssertIntermediateException.
*
*/
@!APILevel[12, atomicservice : true]
public class AssertIntermediateException <: Exception {
    @!APILevel[12, atomicservice : true]
    public let expression: String
    
    @!APILevel[12, atomicservice : true]
    public let originalException: Exception
    
    @!APILevel[12, atomicservice : true]
    public func getOriginalStackTrace(): String
}

// Marker interface to be able to detect BenchInputProvider<T> when we do not know `T`
@!APILevel[12, atomicservice : true]
public interface BenchmarkInputMarker {
}

// Interface to handle benchmarks where some code needs to be executed before the benchmark// or input of the benchmark is mutated and has to be generated each time from scratch.
@!APILevel[12, atomicservice : true]
public interface BenchInputProvider<T> <: BenchmarkInputMarker {
}

// Default and simplest input provider that just copies data for each invokation of the benchmark.
@!APILevel[12, atomicservice : true]
public struct ImmutableInputProvider<T> <: BenchInputProvider<T> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public ImmutableInputProvider(let data: T)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func get(_: Int64): T
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public static func createOrExisting(arg: T, x!:Int64=0): ImmutableInputProvider<T>
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public static func createOrExisting<U>(arg: U): U where U <: BenchInputProvider<T>
}

// Input provider that generates input for the whole benchmark batch in a buffer before executing it
@!APILevel[12, atomicservice : true]
public struct BatchInputProvider<T> <: BenchInputProvider<T> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public BatchInputProvider(let builder: () -> T)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func reset(max: Int64)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func get(idx: Int64): T
}

// Benchmark input provider that generates input right before each execution of benchmark.// The difference with `GenerateEachInputProvider` is that when batch size is 1 we can measure// each benchmark invocation independently so input generation is never included in the measurements.// Should be used if `GenerateEachInputProvider` gives poor quality results.
@!APILevel[12, atomicservice : true]
public struct BatchSizeOneInputProvider<T> <: BenchInputProvider<T> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public BatchSizeOneInputProvider(let builder: () -> T)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func reset(max: Int64)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func get(_: Int64): T
}

// Benchmark input provider that generates input right before each execution of benchmark.// Generation time is included in benchmark measurements // and then later it is excluded from results as part of framework overhead calculations.
@!APILevel[12, atomicservice : true]
public struct GenerateEachInputProvider<T> <: BenchInputProvider<T> {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public GenerateEachInputProvider(let builder: () -> T)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func reset(_: Int64)
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public mut func get(_: Int64): T
}

@!APILevel[12, atomicservice : true]
public interface BenchmarkConfig {
}

extend Configuration <: BenchmarkConfig {
    @!APILevel[12, atomicservice : true]
    public func batchSize(b: Int64)
    
    @!APILevel[12, atomicservice : true]
    public func batchSize(x: Range<Int64>)
    
    @!APILevel[12, atomicservice : true]
    public func warmup(x: Int64)
    
    @!APILevel[12, atomicservice : true]
    public func warmup(x: Duration)
    
    @!APILevel[12, atomicservice : true]
    public func minDuration(x: Duration)
    
    @!APILevel[12, atomicservice : true]
    public func explicitGC(x: ExplicitGcType)
    
    @!APILevel[12, atomicservice : true]
    public func minBatches(x: Int64)
}

@!APILevel[12, atomicservice : true]

extend<T> Iterator<T> {
}

/**
* Interface for all kinds of data that can be collected and analyzed during benchmarking.
*/
@!APILevel[12, atomicservice : true]
public interface Measurement {
}

@!APILevel[12, atomicservice : true]
public struct MeasurementInfo {
}

public type MeasurementUnitTable = Array<(Float64, String)>

extend MeasurementUnitTable {
}

/**
* Measures how much time takes to execute a function.
*/
@!APILevel[12, atomicservice : true]
public struct TimeNow <: Measurement {
    /**
    * @param unit Allows to specify a unit of time that will be used for printing results.
    */
    @!APILevel[12, atomicservice : true]
    public init(unit: ?TimeUnit)
    
    /**
    * Chooses output precision automatically for each case.
    */
    @!APILevel[12, atomicservice : true]
    public init()
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func measure(): Float64
    
    @!APILevel[12, atomicservice : true]
    public prop conversionTable: MeasurementUnitTable
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public prop textDescription: String
}

/**
* Used to explicitly specify the time unit used when `TimeNow` prints time.
*/
@!APILevel[12, atomicservice : true]
public enum TimeUnit <: ToString {
    @!APILevel[12, atomicservice : true]
    Nanos |
    @!APILevel[12, atomicservice : true]
    Micros |
    @!APILevel[12, atomicservice : true]
    Millis |
    @!APILevel[12, atomicservice : true]
    Seconds
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Used to specify what type of GC is invoked by the test framework in benchmarks.
* See std.runtime.GC(heavy: bool) for details about types of GC invokations
*/
@!APILevel[12, atomicservice : true]
public enum ExplicitGcType <: ToString {
    @!APILevel[12, atomicservice : true]
    // GC is not invoked explicitly
    Disabled |
    @!APILevel[12, atomicservice : true]
    // GC(heavy: false) is invoked after each batch
    Light |
    @!APILevel[12, atomicservice : true]
    // GC(heavy: true) is invoked after each batch
    Heavy
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

// corresponds to HW_REF_CPU_CYCLES Perf measurements
@!APILevel[12, atomicservice : true]
public struct CpuCycles <: Measurement {
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func measure(): Float64
    
    @!APILevel[12, atomicservice : true]
    public func setup()
    
    @!APILevel[12, atomicservice : true]
    public prop conversionTable: MeasurementUnitTable
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public prop textDescription: String
}

// todo check if it can work on ohos, and harmonyos
@!APILevel[12, atomicservice : true]
public struct Perf <: Measurement {
    @!APILevel[12, atomicservice : true]
    public Perf(var counter: PerfCounter)
    
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public func setup()
    
    @Frozen
    @!APILevel[12, atomicservice : true]
    public func measure(): Float64
    
    @!APILevel[12, atomicservice : true]
    public prop conversionTable: MeasurementUnitTable
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public prop textDescription: String
}

// Detailed cache counters are not supported yet
@!APILevel[12, atomicservice : true]
public enum PerfCounter <: ToString {
    @!APILevel[12, atomicservice : true]
    HW_CPU_CYCLES |
    @!APILevel[12, atomicservice : true]
    HW_INSTRUCTIONS |
    @!APILevel[12, atomicservice : true]
    HW_CACHE_REFERENCES |
    @!APILevel[12, atomicservice : true]
    HW_CACHE_MISSES |
    @!APILevel[12, atomicservice : true]
    HW_BRANCH_INSTRUCTIONS |
    @!APILevel[12, atomicservice : true]
    HW_BRANCH_MISSES |
    @!APILevel[12, atomicservice : true]
    HW_BUS_CYCLES |
    @!APILevel[12, atomicservice : true]
    HW_STALLED_CYCLES_FRONTEND |
    @!APILevel[12, atomicservice : true]
    HW_STALLED_CYCLES_BACKEND |
    @!APILevel[12, atomicservice : true]
    HW_REF_CPU_CYCLES |
    @!APILevel[12, atomicservice : true]
    SW_CPU_CLOCK |
    @!APILevel[12, atomicservice : true]
    SW_TASK_CLOCK |
    @!APILevel[12, atomicservice : true]
    SW_PAGE_FAULTS |
    @!APILevel[12, atomicservice : true]
    SW_CONTEXT_SWITCHES |
    @!APILevel[12, atomicservice : true]
    SW_CPU_MIGRATIONS |
    @!APILevel[12, atomicservice : true]
    SW_PAGE_FAULTS_MIN |
    @!APILevel[12, atomicservice : true]
    SW_PAGE_FAULTS_MAJ |
    @!APILevel[12, atomicservice : true]
    SW_EMULATION_FAULTS
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Assertion context base class
* 
* If user wants to create custom assertion he must:
* 1. Annotate function with @CustomAssertion
* 2. Provide `AssertionCtx` as it's FIRST parameter in function parameters list
*/
@!APILevel[12, atomicservice : true]
public class AssertionCtx {
    @!APILevel[12, atomicservice : true]
    public prop caller: String
    
    /**
    * Says whether any error occured during run of assertion
    */
    @!APILevel[12, atomicservice : true]
    public prop hasErrors: Bool
    
    /**
    * Unresolved passed arguments getter
    *
    * @param key: String - if matches with value of argument identifier in function declaration, return unresolved argument
    */
    @!APILevel[12, atomicservice : true]
    public func arg(key: String): String
    
    /**
    * Comma-separated string of arguments
    */
    @!APILevel[12, atomicservice : true]
    public prop args: String
    
    /**
    * Specifies names for accessing unresolved values
    *
    * @param aliases: Array<String> - String aliases for each argument in function declaration.
    *                                 Length of provided array should match with size of paramenter list
    */
    @!APILevel[12, atomicservice : true]
    public func setArgsAliases(aliases: Array<String>): Unit
    
    /**
    * Stores FailCheckResult in local stacktrace
    *
    * @throws AssertException
    */
    @!APILevel[12, atomicservice : true]
    public func fail(message: String): Nothing
    
    /**
    * Stores CustomCheckResult in local stacktrace
    *
    * @throws AssertException
    */
    @!APILevel[12, atomicservice : true]
    public func fail<PP>(pt: PP): Nothing where PP <: PrettyPrintable
    
    @!APILevel[12, atomicservice : true]
    public func failExpect(message: String): Unit
    
    /**
    * Stores CustomCheckResult in local stacktrace
    */
    @!APILevel[12, atomicservice : true]
    public func failExpect<PP>(pt: PP): Unit where PP <: PrettyPrintable
}

/**
* Runs custom assertion specified by @Assert[caller](passedArgs) inside of @Test/@TestCase functions
*
* Used by framework and not considered to be called by end user
* 
* @param passedArgs:   Array<String>       - unresolved passed arguments
* @param caller:       String              - name of called custom assertion with generic parameters if such specified
* @param assert:       (AssertionCtx) -> T - capture of invocation of assertion with right arguments
* @param optParentCtx: ?AssertionCtx       - first argument of @CustomAssertion function
*
* @throws AssertException if any occured in nested checks
*/
@!APILevel[12, atomicservice : true]
public func invokeCustomAssert<T>(passedArgs: Array<String>, caller: String, assert: (AssertionCtx) -> T, optParentCtx!: ?AssertionCtx = None): T

/**
* Runs custom assertion specified by @Assert[caller](passedArgs) inside of @Test/@TestCase functions
*
* Used by framework and not considered to be called by end user
* 
* @param passedArgs:   Array<String>         - unresolved passed arguments
* @param caller:       String                - name of called custom assertion with generic parameters if such specified
* @param expect:       (AssertionCtx) -> Any - capture of invocation of assertion with right arguments
* @param optParentCtx: ?AssertionCtx         - first argument of @CustomAssertion function
*/
@!APILevel[12, atomicservice : true]
public func invokeCustomExpect(passedArgs: Array<String>, caller: String, expect: (AssertionCtx) -> Any, optParentCtx!: ?AssertionCtx = None): Unit

/**
* Create default configuration by reading command-line arguments.
* Any command-line argument given to the test will be turned into a field in configuration according to the rules:
* - kebab-case arguments such as --random-seed are turned into camelCase parameters: randomSeed
* - arguments with no values are turned into bool values: --no-color becomes noColor of type Bool and value true
* - the rest are converted according to the following rules, in this order:
*    - `true` and `false` values are Bool: --feel-good=true becomes field feelGood of type Bool and value true
*    - integer number literals are Int64: --random-seed=3 becomes field randomSeed of type Int64 and value 3
*    - float number literals are Float64: --value-pi=3.14 becomes field valuePi of type Float64 and value 3.14
*    - any other values are considered Strings
*/
@!APILevel[12, atomicservice : true]
public func defaultConfiguration(): Configuration

extend Configuration {
}

extend Configuration {
}

@!APILevel[12, atomicservice : true]
public func entryMain(testPackage: TestPackage): Int64

/*
* Fails the test case.
*/
@!APILevel[12, atomicservice : true]
public func failExpect(message: String): Unit

/*
* Immediately fails the test case.
*/
@!APILevel[12, atomicservice : true]
public func fail(message: String): Nothing

/*
* Immediately fails the test with message that was caught exception differs from expeted.
*/
@!APILevel[12, atomicservice : true]
public func assertCaughtUnexpectedE(
    message: String,
    expectedExceptions: String,
    caughtException: String,
    optParentCtx!: ?AssertionCtx = None
): Nothing

/*
* Fails the test with message that was caught exception differs from expeted.
*/
@!APILevel[12, atomicservice : true]
public func expectCaughtUnexpectedE(
    message: String,
    expectedExceptions: String,
    caughtException: String,
    optParentCtx!: ?AssertionCtx = None
): Unit

extend Configuration {
}

extend UInt8 <: Torcable<UInt8> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(UInt8, UInt8)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: UInt8): ?UInt8
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): UInt8
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): UInt8
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): UInt8
}

extend UInt16 <: Torcable<UInt16> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(UInt16, UInt16)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: UInt16): ?UInt16
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): UInt16
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): UInt16
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): UInt16
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func+(arg: UInt8): UInt16
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func-(arg: UInt8): UInt16
}

extend UInt32 <: Torcable<UInt32> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(UInt32, UInt32)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: UInt32): ?UInt32
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): UInt32
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): UInt32
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): UInt32
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func+(arg: UInt8): UInt32
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func-(arg: UInt8): UInt32
}

extend UInt64 <: Torcable<UInt64> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(UInt64, UInt64)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: UInt64): ?UInt64
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): UInt64
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): UInt64
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): UInt64
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func+(arg: UInt8): UInt64
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func-(arg: UInt8): UInt64
}

extend Float32 <: Torcable<Float32> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(Float32, Float32)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: Float32): ?Float32
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): Float32
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): Float32
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): Float32
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func+(arg: UInt8): Float32
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func-(arg: UInt8): Float32
}

extend Float64 <: Torcable<Float64> {
    @!APILevel[12, atomicservice : true]
    public static func randomRecentEntry(): ?(Float64, Float64)
    
    @!APILevel[12, atomicservice : true]
    public static func randomRecentValueComparedTo(value: Float64): ?Float64
    
    @!APILevel[12, atomicservice : true]
    public static func nextFrom(random: Random): Float64
    
    @!APILevel[12, atomicservice : true]
    public static func suggestedFrom(random: Random): Float64
    
    @!APILevel[12, atomicservice : true]
    public func isZero()
    
    @!APILevel[12, atomicservice : true]
    public func bitFlip(at: UInt8): Float64
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func+(arg: UInt8): Float64
    
    @OverflowWrapping
    @!APILevel[12, atomicservice : true]
    public operator func-(arg: UInt8): Float64
}

@!APILevel[12, atomicservice : true]
public struct KeyHelp <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop help: KeyHelp
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyNoColor <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop noColor: KeyNoColor
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyFromTopLevel <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop fromTopLevel: KeyFromTopLevel
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyRandomSeed <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop randomSeed: KeyRandomSeed
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyWarmup <: KeyFor<Int64> & KeyFor<Duration> {
    @!APILevel[12, atomicservice : true]
    static public prop warmup: KeyWarmup
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyMinDuration <: KeyFor<Duration> {
    @!APILevel[12, atomicservice : true]
    static public prop minDuration: KeyMinDuration
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyExplicitGC <: KeyFor<ExplicitGcType> {
    @!APILevel[12, atomicservice : true]
    static public prop explicitGC: KeyExplicitGC
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyMinBatches <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop minBatches: KeyMinBatches
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyBatchSize <: KeyFor<Int64> & KeyFor<Range<Int64>> {
    @!APILevel[12, atomicservice : true]
    static public prop batchSize: KeyBatchSize
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyParallel <: KeyFor<Bool> & KeyFor<String> & KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop parallel: KeyParallel
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyGenerationSteps <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop generationSteps: KeyGenerationSteps
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyReductionSteps <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop reductionSteps: KeyReductionSteps
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeySkip <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop skip: KeySkip
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuided <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuided: KeyCoverageGuided
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuidedInitialSeeds <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuidedInitialSeeds: KeyCoverageGuidedInitialSeeds
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuidedMaxCandidates <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuidedMaxCandidates: KeyCoverageGuidedMaxCandidates
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuidedBaselineScore <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuidedBaselineScore: KeyCoverageGuidedBaselineScore
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuidedNewCoverageScore <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuidedNewCoverageScore: KeyCoverageGuidedNewCoverageScore
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCoverageGuidedNewCoverageBonus <: KeyFor<Int64> {
    @!APILevel[12, atomicservice : true]
    static public prop coverageGuidedNewCoverageBonus: KeyCoverageGuidedNewCoverageBonus
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyBench <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop bench: KeyBench
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyTimeout <: KeyFor<Duration> {
    @!APILevel[12, atomicservice : true]
    static public prop timeout: KeyTimeout
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyTimeoutEach <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop timeoutEach: KeyTimeoutEach
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyCaptureOutput <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop captureOutput: KeyCaptureOutput
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyNoCaptureOutput <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop noCaptureOutput: KeyNoCaptureOutput
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyShowAllOutput <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop showAllOutput: KeyShowAllOutput
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyVerbose <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop verbose: KeyVerbose
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyMeasurement <: KeyFor<Measurement> {
    @!APILevel[12, atomicservice : true]
    static public prop measurement: KeyMeasurement
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyMeasurementInfo <: KeyFor<MeasurementInfo> {
    @!APILevel[12, atomicservice : true]
    static public prop measurementInfo: KeyMeasurementInfo
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyInternalTestrunnerInputPath <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop internalTestrunnerInputPath: KeyInternalTestrunnerInputPath
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyDeathAware <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop deathAware: KeyDeathAware
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyBaseline <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop baseline: KeyBaseline
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyFilter <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop filter: KeyFilter
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyIncludeTags <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop includeTags: KeyIncludeTags
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyExcludeTags <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop excludeTags: KeyExcludeTags
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyReportPath <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop reportPath: KeyReportPath
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyReportFormat <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop reportFormat: KeyReportFormat
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyBaselinePath <: KeyFor<String> {
    @!APILevel[12, atomicservice : true]
    static public prop baselinePath: KeyBaselinePath
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

@!APILevel[12, atomicservice : true]
public struct KeyDryRun <: KeyFor<Bool> {
    @!APILevel[12, atomicservice : true]
    static public prop dryRun: KeyDryRun
    
    @!APILevel[12, atomicservice : true]
    public prop name: String
}

extend Configuration {
}

extend Process {
}

extend Process {
}

@!APILevel[12, atomicservice : true]
public class PowerAssertDiagramBuilder {
    @!APILevel[12, atomicservice : true]
    public init(expression: String)
    
    // Below are shortcuts to reduce the size of generated code by power assertion macro
    @!APILevel[12, atomicservice : true]
    public func r<T>(value: T, exprAsText: String, position: Int64): T
    
    @!APILevel[12, atomicservice : true]
    public func r(value: Rune, exprAsText: String, position: Int64): Rune
    
    @!APILevel[12, atomicservice : true]
    public func r(value: String, exprAsText: String, position: Int64): String
    
    @!APILevel[12, atomicservice : true]
    public func h(exception: Exception, exprAsText: String, position: Int64): Nothing
    
    @!APILevel[12, atomicservice : true]
    public func w(passed: Bool): Unit
}

extend<T> Range<T> where T <: Comparable<T> {
}

extend Process {
}

extend SubProcess {
}

extend TestGroupResult {
}

extend TestSuiteResult {
}

extend StringBuilder {
}

extend TestGroupResult {
}

extend StatisticsSample {
}

extend BenchmarkResult {
}

extend JsonObject {
}

extend<V> Iterator<V> {
}

extend String {
}

extend TestSuiteId {
}

extend TestGroupResult {
}

extend TestSuiteId {
}

@!APILevel[12, atomicservice : true]
sealed interface Reporter<TReport, TReturn> {
}

// NOTE: our predefined reporters
@!APILevel[12, atomicservice : true]
public class CsvReporter <: Reporter<BenchReport, Unit> {
    @!APILevel[12, atomicservice : true]
    public CsvReporter(let directory: Path)
}

@!APILevel[12, atomicservice : true]
public class CsvRawReporter <: Reporter<BenchReport, Unit> {
    @!APILevel[12, atomicservice : true]
    public CsvRawReporter(let directory: Path)
}

@!APILevel[12, atomicservice : true]
public class XmlReporter <: Reporter<TestReport, Unit> {
    @!APILevel[12, atomicservice : true]
    public XmlReporter(let directory: Path)
}

@!APILevel[12, atomicservice : true]
public class ConsoleReporter <: Reporter<TestReport, Unit> & Reporter<BenchReport, Unit> {
    @!APILevel[12, atomicservice : true]
    public ConsoleReporter(let colored!: Bool = true)
}

@!APILevel[12, atomicservice : true]
public class TextReporter<PP> <: Reporter<TestReport, PP> & Reporter<BenchReport, PP>
        where PP <: PrettyPrinter {
    @!APILevel[12, atomicservice : true]
    public TextReporter(let into!: PP)
}

extend <PP> TextReporter<PP> <: TextReporterBase<PP> where PP <: PrettyPrinter {
    @!APILevel[12, atomicservice : true]
    public func printReport(report: Report): PP
}

extend ConsoleReporter <: TextReporterBase<Unit> {
    @!APILevel[12, atomicservice : true]
    public func printReport(report: Report): Unit
}

// NOTE: for our internal benchmarks
@!APILevel[12, atomicservice : true]
public class RawStatsReporter <: Reporter<BenchReport, HashMap<String, (Float64, Float64)>> {
    @!APILevel[12, atomicservice : true]
    public RawStatsReporter()
}

extend LStep <: Serializable<LStep> {
    @!APILevel[12, atomicservice : true]
    public func serializeInternal(): DataModel
    
    @!APILevel[12, atomicservice : true]
    public static func deserialize(dm: DataModel): LStep
}

extend StepKind <: Serializable<StepKind> {
    @!APILevel[12, atomicservice : true]
    public func serializeInternal(): DataModel
    
    @!APILevel[12, atomicservice : true]
    public static func deserialize(dm: DataModel): StepKind
}

extend StepInfo <: Serializable<StepInfo> {
    @!APILevel[12, atomicservice : true]
    public func serializeInternal(): DataModel
    
    @!APILevel[12, atomicservice : true]
    public static func deserialize(dm: DataModel): StepInfo
}

extend RunStepResult {
}

extend TestCaseResult {
}

extend TestSuiteResult {
}

extend TestGroupResult {
}

extend RenderOptions <: Serializable<RenderOptions> {
    @!APILevel[12, atomicservice : true]
    public func serializeInternal(): DataModel
    
    @!APILevel[12, atomicservice : true]
    public static func deserialize(dm: DataModel): RenderOptions
}

extend Float64 <: AsFloat {
    @!APILevel[12, atomicservice : true]
    public func asFloat(): Float64
}

extend<T> Iterator<T> where T <: AsFloat {
}

@!APILevel[12, atomicservice : true]
public class FlatMapProcessor<T,R> <: DataStrategyProcessor<R> {
}

@!APILevel[12, atomicservice : true]
public class FlatMapStrategyProcessor<T,R> <: DataStrategyProcessor<R> {
}

@!APILevel[12, atomicservice : true]
public class MapProcessor<T,R> <: DataStrategyProcessor<R> {
}

@!APILevel[12, atomicservice : true]
public class CartesianProductProcessor<T0,T1> <: DataStrategyProcessor<(T0,T1)> {
    @!APILevel[12, atomicservice : true]
    public CartesianProductProcessor(let left: DataStrategyProcessor<T0>, let right: DataStrategyProcessor<T1>)
}

@!APILevel[12, atomicservice : true]
public class SimpleProcessor<T> <: DataStrategyProcessor<T> {
    @!APILevel[12, atomicservice : true]
    public SimpleProcessor(let buildDelegate: () -> DataStrategy<T>, let name: String )
}

// Intrusive cyclic doubly linked list// Used to advance type-erased internal lazy iterators one after another in a cycle//// Public only due to protected sealed functions being implicitly public
@!APILevel[12, atomicservice : true]
public open class LazyCyclicNode {
}

// public only due to protected sealed functions being implicitly public
@!APILevel[12, atomicservice : true]
public class InputParameter {
}

@!APILevel[12, atomicservice : true]

/**
* Base class for all combinators of `DataStategy`es.
*/
@!APILevel[12, atomicservice : true]
abstract sealed class DataStrategyProcessor<T> {
    @!APILevel[12, atomicservice : true]
    public func intoBenchmark(
        caseName!: String,
        configuration!: Configuration,
        doRun!: (T, Int64, Int64) -> Float64
    ): Benchmark
    
    @!APILevel[12, atomicservice : true]
    public func intoUnitTestCase(
        caseName!: String,
        configuration!: Configuration,
        doRun!: (T) -> Unit
    ): UnitTestCase
    
    /** Starting point for combinding/mapping `DataStategy`es.
    * 
    *  @param s DataStrategy to wrap
    *  @param name Name of the corresponding parameter in testing report
    */
    @!APILevel[12, atomicservice : true]
    public static func start(s: DataStrategy<T>, name: String): SimpleProcessor<T>
    
    // Overload used for type checking and lazy creation inside macro generated code
    @!APILevel[12, atomicservice : true]
    public static func start<U>(f: () -> DataStrategy<U>, name: String): DataStrategyProcessor<U>
        where U <: BenchInputProvider<T>
   
    
    // Overload used for type checking and lazy creation inside macro generated code
    @!APILevel[12, atomicservice : true]
    public static func start(f: () -> DataStrategy<T>, name: String, x!: Int64 = 0): SimpleProcessor<T>
    
    // Overload used for type checking and lazy creation inside macro generated code
    @!APILevel[12, atomicservice : true]
    public static func start(f: () -> DataStrategyProcessor<T>, _: String): DataStrategyProcessor<T>
    
    // Overload used for type checking and lazy creation inside macro generated code
    @!APILevel[12, atomicservice : true]
    public static func start<U>(f: () -> DataStrategyProcessor<U>, _: String, x!: Int64 = 0): DataStrategyProcessor<U>
        where U <: BenchInputProvider<T>
   
}

extend<T> DataStrategyProcessor<T> {
    /**
    * "map" combinator that simply applies `f` to every item of original data strategy.
    * Shrinking also happens on original input and then mapped.
    */
    @!APILevel[12, atomicservice : true]
    public func map<R>(f: (T) -> R): MapProcessor<T, R>
    
    /**
    * "map" combinator with access to current `Configuration` that simply applies `f` to every item of original data strategy.
    * Shrinking also happens on original input and then flat mapped.
    */
    @!APILevel[12, atomicservice : true]
    public func mapWithConfig<R>(f: (T, Configuration) -> R): MapProcessor<T, R>
    
    /**
    * "flat map" combinator that simply applies `f` to every item of original data strategy.
    * Shrinking also happens on original input and then flat mapped.
    */
    @!APILevel[12, atomicservice : true]
    public func flatMap<R>(f: (T) -> DataProvider<R>): FlatMapProcessor<T, R>
    
    /**
    * "flat map" combinator that simply applies `f` to every item of original data strategy.
    * However shrinking is done by returned strategy rather than original input.
    */
    @!APILevel[12, atomicservice : true]
    public func flatMapStrategy<R>(f: (T) -> DataStrategy<R>): FlatMapStrategyProcessor<T, R>
    
    /**
    * Cartesian product combinator. Creates data strategy that contains all possible permutations of elements in original strategies.
    * Shrinking happens on each element of the original strategy independently and uniformly.
    */
    @!APILevel[12, atomicservice : true]
    public func product<R>(p: DataStrategyProcessor<R>): CartesianProductProcessor<T, R>
    
    /**
    * Convenience adapter for `DataStrategyProcessor.product` when second strategy only does side effects. 
    * Intended to be used from code generated by `@Test` macro to help with type inference. 
    */
    @!APILevel[12, atomicservice : true]
    public func productWithUnit<P>(p: P): MapProcessor<(T, Unit), T> where P <: DataStrategyProcessor<Unit>
}

extend JsonValue <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend String <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend Int64 <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend<T> Option<T> <: IntoJson where T <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend<T> Array<T> <: IntoJson where T <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend<T> ArrayList<T> <: IntoJson where T <: IntoJson {
    @!APILevel[12, atomicservice : true]
    public func json(): JsonValue
}

extend CheckResult <: PrettyPrintable {
    @!APILevel[12, atomicservice : true]
    public func pprintWithFailedPrefix(pp: PrettyPrinter): PrettyPrinter
    
    @!APILevel[12, atomicservice : true]
    public func pprint(pp: PrettyPrinter): PrettyPrinter
}

@!APILevel[12, atomicservice : true]
public open class UnittestException <: Exception {
}

@!APILevel[12, atomicservice : true]
public class UnittestCliOptionsFormatException <: UnittestException {
}

