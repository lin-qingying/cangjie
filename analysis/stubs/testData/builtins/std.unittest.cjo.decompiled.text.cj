// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest

import std.collection.ArrayList
import std.core.*
import std.unittest.diff.AssertPrintable
import std.unittest.common.*
import std.time.DateTime
import std.unittest.common.PrettyText
import std.collection.*
import std.math.abs
import std.runtime.ProcessorInfo
import std.random.Random
import std.unittest.common.Color
import std.convert.Formattable
import std.math.*
import std.runtime.GC
import std.sort.*
import std.sync.AtomicOptionReference
import std.process.Process
import std.unicode.*
import std.unittest.prop_test.*
import std.unittest.common.KeyTags
import std.fs.*
import std.process.*
import std.sync.Semaphore
import std.collection.concurrent.ConcurrentLinkedQueue
import std.sync.ReentrantMutex
import std.unittest.common.optionsInfo
import std.unittest.prop_test.RandomSource
import std.math.round
import std.regex.Regex
import std.convert.Parsable
import std.unittest.common.Configuration
import std.unittest.common.ConfigurationKey
import std.unittest.common.DataProvider
import std.unittest.common.DataStrategy
import std.unittest.common.DataShrinker
import std.unittest.common.registerOptionValidator
import std.unittest.common.setOptionInfo
import std.unittest.common.OptionValidity
import std.unittest.common.KeyFor
import std.unittest.prop_test.Arbitrary
import std.unittest.prop_test.Shrink
import std.unittest.prop_test.random
import std.unittest.prop_test.KeyRandom
import std.unittest.mock.PrettyException
import std.fs.Path
import std.unittest.common.PrettyPrinter
import std.collection.concurrent.NonBlockingQueue
import std.env.atExit
import std.sync.AtomicBool
import std.time.MonoTime
import std.time.*
import std.sort.SortExtension
import std.sync.*
import std.unittest.prop_test.ShrinkHelpers
import std.unittest.common.toStringOrPlaceholder
import std.unittest.mock.*
import std.collection.concurrent.LinkedBlockingQueue
import std.env.exit
import std.env.getTempDirectory
import std.binary.BigEndianOrder
import std.io.IOException
import std.net.*
import std.core.println as corePrintln
import std.core.print as corePrint

let MEASUREMENTS_LIMIT: Int64 = COMPILED_CODE /* compiled code */

let MAX_BATCH_SIZE: Int64 = COMPILED_CODE /* compiled code */

let MIN_COLUMN_WIDTH: Int64 = COMPILED_CODE /* compiled code */

public struct ImmutableInputProvider<T> <: std.unittest.BenchInputProvider<T> {
    @Frozen
    public mut func get(_: Int64): T { /* compiled code */ }

    @Frozen
    public static func createOrExisting(arg: T, x!: Int64 = COMPILED_CODE /* compiled code */): std.unittest.ImmutableInputProvider<T> { /* compiled code */ }

    @Frozen
    public static func createOrExisting<U>(arg: U): U { /* compiled code */ }

    internal let data: T

    @Frozen
    ImmutableInputProvider(data: T) { /* compiled code */ }
}

public interface BenchInputProvider<T> <: std.unittest.BenchmarkInputMarker {
    @Frozen
    public open mut func reset(max: Int64): Unit { /* compiled code */ }

    @Frozen
    public open mut func get(idx: Int64): T
}

public struct BatchInputProvider<T> <: std.unittest.BenchInputProvider<T> {
    internal var data: std.core.Array<T> = COMPILED_CODE /* compiled code */

    @Frozen
    public mut func reset(max: Int64): Unit { /* compiled code */ }

    @Frozen
    public mut func get(idx: Int64): T { /* compiled code */ }

    internal let builder: () -> T

    @Frozen
    BatchInputProvider(builder: () -> T) { /* compiled code */ }
}

public struct BatchSizeOneInputProvider<T> <: std.unittest.BenchInputProvider<T> {
    internal var data: T

    @Frozen
    public mut func reset(max: Int64): Unit { /* compiled code */ }

    @Frozen
    public mut func get(_: Int64): T { /* compiled code */ }

    internal let builder: () -> T

    @Frozen
    BatchSizeOneInputProvider(builder: () -> T) { /* compiled code */ }
}

public struct GenerateEachInputProvider<T> <: std.unittest.BenchInputProvider<T> {
    @Frozen
    public mut func reset(_: Int64): Unit { /* compiled code */ }

    @Frozen
    public mut func get(_: Int64): T { /* compiled code */ }

    internal let builder: () -> T

    @Frozen
    GenerateEachInputProvider(builder: () -> T) { /* compiled code */ }
}

internal class TestSuiteResult <: std.unittest.ResultContainer<std.unittest.TestCaseResult> {
    internal func merge(other: std.unittest.TestSuiteResult): Unit { /* compiled code */ }

    internal static func mergeAll(suiteResults: std.core.Iterable<std.unittest.TestSuiteResult>): std.core.Collection<std.unittest.TestSuiteResult> { /* compiled code */ }

    internal let suiteId: std.unittest.TestSuiteId

    internal let suiteInfo: std.unittest.TestSuiteReportInfo

    TestSuiteResult(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo) { /* compiled code */ }
}

internal interface BenchTable {
    public static func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable> { /* compiled code */ }

    public open func doBuild(): std.unittest.PrettyTable

    public open prop hasBaselineHeader: Bool
}

private open class BenchTableImpl <: std.core.Object & std.unittest.BenchTable {
    protected let subgroups: std.collection.ArrayList<std.unittest.BenchTableImpl>

    public open prop hasBaselineHeader: Bool {
        get() { /* compiled code */ }
    }

    public open func putResultToBaselineReport(key: std.core.String, value: Float64): Unit { /* compiled code */ }

    public open func getResultFromBaselineReport(key: std.core.String): std.core.Option<Float64> { /* compiled code */ }

    public static func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable> { /* compiled code */ }

    protected open func isEmpty(): Bool { /* compiled code */ }

    internal func initTableStructure(): Unit { /* compiled code */ }

    public func doBuild(): std.unittest.PrettyTable { /* compiled code */ }

    protected open func doTableBuild(): Unit { /* compiled code */ }

    protected func addSubgroup(subgroup: std.unittest.BenchTableImpl): Unit { /* compiled code */ }

    protected let props: std.unittest.BenchTableProperties

    protected let table: std.unittest.PrettyTable

    BenchTableImpl(props!: std.unittest.BenchTableProperties = COMPILED_CODE /* compiled code */, table!: std.unittest.PrettyTable = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal foreign func GetRdtscp(): UInt64

internal interface TestFilter {
    public open func shouldSkipTestClass(suiteFilterKey: std.unittest.SuiteFilterKey): Bool

    public open func shouldSkipTestCase(caseFilterKey: std.unittest.CaseFilterKey): Bool
}

internal class CompositeTestFilter <: std.core.Object & std.unittest.TestFilter {
    public override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    public override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    private let filters: std.core.Array<std.unittest.TestFilter>

    CompositeTestFilter(filters: std.core.Array<std.unittest.TestFilter>) { /* compiled code */ }
}

internal class TagsTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    private let includeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    private let excludeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    init(includeArgs!: std.core.Array<std.core.String> = COMPILED_CODE /* compiled code */, excludeArgs!: std.core.Array<std.core.String> = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public redef static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TagsTestFilter { /* compiled code */ }

    public redef static func fromDefaultConfiguration(): std.unittest.TagsTestFilter { /* compiled code */ }

    public override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    public override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }
}

internal class FilterOptionTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    private let includeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    private let excludeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    init() { /* compiled code */ }

    init(filterArgs: std.core.Array<std.core.String>) { /* compiled code */ }

    public redef static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.FilterOptionTestFilter { /* compiled code */ }

    public redef static func fromDefaultConfiguration(): std.unittest.FilterOptionTestFilter { /* compiled code */ }

    public override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    public override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }
}

internal interface UserTestFilter <: std.unittest.TestFilter {
    public static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TestFilter { /* compiled code */ }

    public static func fromDefaultConfiguration(): std.unittest.TestFilter { /* compiled code */ }
}

public func defaultConfiguration(): std.unittest.common.Configuration { /* compiled code */ }

internal struct SuiteFilterKey {
    internal static func fromTestSuite(groupName: std.core.String, suite: std.unittest.TestSuite): std.unittest.SuiteFilterKey { /* compiled code */ }

    internal let suiteId: std.unittest.TestSuiteId

    internal let tags: std.core.Array<std.core.String>

    internal let topLevelFunc: std.core.Option<std.core.String>

    SuiteFilterKey(suiteId: std.unittest.TestSuiteId, tags!: std.core.Array<std.core.String> = COMPILED_CODE /* compiled code */, topLevelFunc!: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

public class TestSuite {
    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public func runTests(): std.unittest.TestReport { /* compiled code */ }

    public func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }

    public func runBenchmarks(): std.unittest.BenchReport { /* compiled code */ }

    public func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    internal func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult { /* compiled code */ }

    internal func execute(groupName: std.core.String, runConfiguration: std.unittest.common.Configuration, filterService: std.unittest.FilterService): std.unittest.TestSuiteResult { /* compiled code */ }

    public static func builder(name: std.core.String): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public static func builder(suite: std.unittest.TestSuite): std.unittest.TestSuiteBuilder { /* compiled code */ }

    private let _name: std.core.String

    internal let cases: std.core.Array<std.unittest.CaseOrBench>

    internal let suiteConfiguration: std.unittest.common.Configuration

    internal let beforeAlls: std.core.Array<() -> Unit>

    internal let beforeEachs: std.core.Array<(std.core.String) -> Unit>

    internal let afterEachs: std.core.Array<(std.core.String) -> Unit>

    internal let afterAlls: std.core.Array<() -> Unit>

    internal let template: std.core.Option<std.unittest.TestSuite>

    TestSuite(_name: std.core.String, cases: std.core.Array<std.unittest.CaseOrBench>, suiteConfiguration: std.unittest.common.Configuration, beforeAlls: std.core.Array<() -> Unit>, beforeEachs: std.core.Array<(std.core.String) -> Unit>, afterEachs: std.core.Array<(std.core.String) -> Unit>, afterAlls: std.core.Array<() -> Unit>, template: std.core.Option<std.unittest.TestSuite>) { /* compiled code */ }
}

internal interface ReportFormat {
    public static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    public static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public static func fromDefaultConfiguration(): std.core.Option<std.unittest.ReportFormat> { /* compiled code */ }

    public open func report(testInfo: std.unittest.TestGroupResult): Unit
}

internal enum BenchReportFormat <: std.unittest.ReportFormat {
    | CSV(std.fs.Path)

    | CSV_RAW(std.fs.Path)

    | Html(std.fs.Path)

    public redef static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    public redef static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public redef static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public func report(result: std.unittest.TestGroupResult): Unit { /* compiled code */ }
}

internal enum UTestReportFormat <: std.unittest.ReportFormat {
    | XML(std.fs.Path)

    public redef static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public redef static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    public redef static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    public func report(result: std.unittest.TestGroupResult): Unit { /* compiled code */ }
}

public struct KeyReportPath <: std.unittest.common.KeyFor<std.core.String> {
    public static prop reportPath: std.unittest.KeyReportPath {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal func getReportDirectory<Key>(key: Key): std.core.Option<std.fs.Path> { /* compiled code */ }

public struct KeyReportFormat <: std.unittest.common.KeyFor<std.core.String> {
    public static prop reportFormat: std.unittest.KeyReportFormat {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public class UnittestCliOptionsFormatException <: std.unittest.UnittestException {
    init(optionName: std.core.String, actual!: std.core.String, expected!: std.core.String) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal interface Result <: std.unittest.common.Serializable<std.unittest.Result> {
    public open func visitChildren(_: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    public open func walk(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    public open func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.Result { /* compiled code */ }

    public open prop details: std.unittest.Details
}

internal class RunStepResult <: std.core.Object & std.unittest.Result {
    public prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    internal static func simpleError(step: std.unittest.StepKind, check: std.unittest.CheckResult): std.unittest.RunStepResult { /* compiled code */ }

    public func hasFailures(): Bool { /* compiled code */ }

    public prop checkResults: std.core.Array<std.unittest.CheckResult> {
        get() { /* compiled code */ }
    }

    public func statusCode(): std.unittest.TestCode { /* compiled code */ }

    internal let checksPassed: Int64

    internal let startTimestamp: std.time.DateTime

    internal let kind: std.unittest.StepKind

    internal let info: std.unittest.StepInfo

    internal let duration: std.core.Duration

    RunStepResult(checksPassed: Int64, startTimestamp: std.time.DateTime, kind: std.unittest.StepKind, info: std.unittest.StepInfo, duration!: std.core.Duration = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class TestCaseResult <: std.unittest.ResultContainer<std.unittest.RunStepResult> {
    internal static func fromSingleStep(id: std.unittest.TestCaseId, info: std.unittest.TestCaseReportInfo, step: std.unittest.RunStepResult): std.unittest.TestCaseResult { /* compiled code */ }

    private var _output: std.unittest.CapturedOutput

    internal prop output: std.unittest.CapturedOutput {
        get() { /* compiled code */ }
    }

    public func addOutput(output: std.unittest.CapturedOutput): Unit { /* compiled code */ }

    public func addSuccessfullStep(next: std.unittest.RunStepResult): Unit { /* compiled code */ }

    public func hasFailures(): Bool { /* compiled code */ }

    internal let caseId: std.unittest.TestCaseId

    internal let caseInfo: std.unittest.TestCaseReportInfo

    internal let renderOptions: std.unittest.RenderOptions

    TestCaseResult(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, renderOptions: std.unittest.RenderOptions) { /* compiled code */ }
}

internal class TestGroupResult <: std.unittest.ResultContainer<std.unittest.GroupResultPart> {
    internal static func fromPackageSuites(packageName: std.core.String, suiteResults: std.core.Collection<std.unittest.TestSuiteResult>, processErrors: std.core.Array<std.unittest.TestProcessError>, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>): std.unittest.TestGroupResult { /* compiled code */ }

    internal prop hasProcessError: Bool {
        get() { /* compiled code */ }
    }

    internal func add(testSuite: std.unittest.Result): Unit { /* compiled code */ }

    internal let groupName: std.core.String

    internal let nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>

    TestGroupResult(groupName: std.core.String, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>) { /* compiled code */ }
}

internal class Details {
    internal var failedCount: Int64

    internal var passedCount: Int64

    internal var skippedCount: Int64

    internal var errorCount: Int64

    internal var executedSteps: Int64

    internal var failedSteps: Int64

    internal var startTimestamp: std.time.DateTime

    internal var duration: std.core.Duration

    init() { /* compiled code */ }

    internal func add(other: std.unittest.Details): Unit { /* compiled code */ }

    internal prop totalCount: Int64 {
        get() { /* compiled code */ }
    }

    internal prop isSuccess: Bool {
        get() { /* compiled code */ }
    }

    internal var testcode: std.unittest.TestCode

    internal var isAccountedStep: Bool

    Details(testcode: std.unittest.TestCode, isAccountedStep: Bool) { /* compiled code */ }
}

internal class RememberingDataProvider<T> <: std.core.Object & std.unittest.common.DataProvider<T> {
    internal var info: std.unittest.InputParameter

    internal var current: std.core.Option<T> = COMPILED_CODE /* compiled code */

    public func provide(): std.core.Iterable<T> { /* compiled code */ }

    internal let delegate: std.unittest.common.DataProvider<T>

    RememberingDataProvider(name: std.core.String, delegate: std.unittest.common.DataProvider<T>) { /* compiled code */ }
}

public struct MeasurementInfo {
    public let conversionTable: std.core.Array<(Float64, std.core.String)>

    public let name: std.core.String

    public let textDescription: std.core.String

    MeasurementInfo(conversionTable: std.core.Array<(Float64, std.core.String)>, name: std.core.String, textDescription: std.core.String) { /* compiled code */ }
}

public class TestReport <: std.unittest.Report {
    public func reportTo<T>(reporter: std.unittest.Reporter<std.unittest.TestReport, T>): T { /* compiled code */ }

    TestReport(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

public class BenchReport <: std.unittest.Report {
    public func reportTo<T>(reporter: std.unittest.Reporter<std.unittest.BenchReport, T>): T { /* compiled code */ }

    BenchReport(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

internal enum ExecutionMode {
    | OnlyTests

    | OnlyBenchmarks

    internal func asFilter(): std.unittest.FrameworkTestFilter { /* compiled code */ }
}

internal struct FilterService {
    internal func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.unittest.FilterService { /* compiled code */ }

    internal let userFilter: std.unittest.TestFilter

    internal let frameworkFilter: std.unittest.FrameworkTestFilter

    FilterService(userFilter: std.unittest.TestFilter, frameworkFilter: std.unittest.FrameworkTestFilter) { /* compiled code */ }
}

public class TestSuiteBuilder {
    internal let beforeAlls: std.collection.ArrayList<() -> Unit>

    internal let beforeEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    internal let afterEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    internal let afterAlls: std.collection.ArrayList<() -> Unit>

    internal var configuration: std.unittest.common.Configuration

    internal var tests: std.collection.ArrayList<std.unittest.CaseOrBench>

    internal var template_: std.core.Option<std.unittest.TestSuite>

    public func setName(name: std.core.String): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func configure(configuration: std.unittest.common.Configuration): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func beforeEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func afterEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func beforeEach(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func afterEach(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func beforeAll(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func afterAll(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func add(test: std.unittest.UnitTestCase): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func add(benchmark: std.unittest.Benchmark): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func template(template: std.unittest.TestSuite): std.unittest.TestSuiteBuilder { /* compiled code */ }

    public func build(): std.unittest.TestSuite { /* compiled code */ }

    private var name: std.core.String

    TestSuiteBuilder(name: std.core.String) { /* compiled code */ }
}

internal enum CaseOrBench {
    | Case(std.unittest.UnitTestCase)

    | Bench(std.unittest.Benchmark)

    internal prop caseConfiguration: std.unittest.common.Configuration {
        get() { /* compiled code */ }
    }

    internal prop name: std.core.String {
        get() { /* compiled code */ }
    }

    internal prop isBench: Bool {
        get() { /* compiled code */ }
    }

    internal prop executor: std.unittest.Executor {
        get() { /* compiled code */ }
    }
}

public class TestGroup {
    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public func runTests(): std.unittest.TestReport { /* compiled code */ }

    public func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }

    public func runBenchmarks(): std.unittest.BenchReport { /* compiled code */ }

    public func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    internal func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult { /* compiled code */ }

    public static func builder(name: std.core.String): std.unittest.TestGroupBuilder { /* compiled code */ }

    public static func builder(group: std.unittest.TestGroup): std.unittest.TestGroupBuilder { /* compiled code */ }

    private let _name: std.core.String

    internal let suites: std.core.Array<std.unittest.TestSuite>

    internal let groupConfiguration: std.unittest.common.Configuration

    TestGroup(_name: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, groupConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

public class TestGroupBuilder {
    private let suites: std.collection.ArrayList<std.unittest.TestSuite>

    private var configuration: std.unittest.common.Configuration

    public func setName(name: std.core.String): std.unittest.TestGroupBuilder { /* compiled code */ }

    public func configure(configuration: std.unittest.common.Configuration): std.unittest.TestGroupBuilder { /* compiled code */ }

    public func add(test: std.unittest.UnitTestCase): std.unittest.TestGroupBuilder { /* compiled code */ }

    public func add(benchmark: std.unittest.Benchmark): std.unittest.TestGroupBuilder { /* compiled code */ }

    public func add(suite: std.unittest.TestSuite): std.unittest.TestGroupBuilder { /* compiled code */ }

    public func build(): std.unittest.TestGroup { /* compiled code */ }

    private var name: std.core.String

    TestGroupBuilder(name: std.core.String) { /* compiled code */ }
}

public class UnitTestCase {
    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public func run(): std.unittest.TestReport { /* compiled code */ }

    public static func createParameterized<T>(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, body!: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    public static func createParameterized<T>(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, body!: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    public static func create(name: std.core.String, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, body!: () -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    private let _name: std.core.String

    internal let caseConfiguration: std.unittest.common.Configuration

    internal let executor: std.unittest.Executor

    UnitTestCase(_name: std.core.String, caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

public class Benchmark {
    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public func run(): std.unittest.BenchReport { /* compiled code */ }

    internal static func createWithExplicitLoop<T>(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, optLambda: (T, Int64, Int64) -> Float64): std.unittest.Benchmark { /* compiled code */ }

    public static func createParameterized<T>(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, measurement!: std.unittest.Measurement = COMPILED_CODE /* compiled code */, body!: (T) -> Unit): std.unittest.Benchmark { /* compiled code */ }

    public static func createParameterized<T>(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, measurement!: std.unittest.Measurement = COMPILED_CODE /* compiled code */, body!: (T) -> Unit): std.unittest.Benchmark { /* compiled code */ }

    public static func create(name: std.core.String, configuration!: std.unittest.common.Configuration = COMPILED_CODE /* compiled code */, measurement!: std.unittest.Measurement = COMPILED_CODE /* compiled code */, body!: () -> Unit): std.unittest.Benchmark { /* compiled code */ }

    private let _name: std.core.String

    internal let benchmarkConfiguration: std.unittest.common.Configuration

    internal let executor: std.unittest.Executor

    Benchmark(_name: std.core.String, benchmarkConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

public abstract open class DataStrategyProcessor<T> {
    private var genIterator: std.core.Iterator<T> = COMPILED_CODE /* compiled code */

    internal var currentReduction: Int64 = COMPILED_CODE /* compiled code */

    protected prop isInfinite: Bool

    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T>

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T>

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter>

    protected func lastItem(configuration: std.unittest.common.Configuration): T

    internal func nextIteration(configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    internal func processNextWith(_: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.unittest.ProcessorResult { /* compiled code */ }

    internal func shrink(configuration: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.core.Option<std.unittest.ProcessorResult> { /* compiled code */ }

    public func intoBenchmark(caseName!: std.core.String, configuration!: std.unittest.common.Configuration, doRun!: (T, Int64, Int64) -> Float64): std.unittest.Benchmark { /* compiled code */ }

    public func intoUnitTestCase(caseName!: std.core.String, configuration!: std.unittest.common.Configuration, doRun!: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    public static func start(s: std.unittest.common.DataStrategy<T>, name: std.core.String): std.unittest.SimpleProcessor<T> { /* compiled code */ }

    public static func start<U>(f: () -> std.unittest.common.DataStrategy<U>, name: std.core.String): std.unittest.DataStrategyProcessor<U> { /* compiled code */ }

    public static func start(f: () -> std.unittest.common.DataStrategy<T>, name: std.core.String, x!: Int64 = COMPILED_CODE /* compiled code */): std.unittest.SimpleProcessor<T> { /* compiled code */ }

    public static func start(f: () -> std.unittest.DataStrategyProcessor<T>, _: std.core.String): std.unittest.DataStrategyProcessor<T> { /* compiled code */ }

    public static func start<U>(f: () -> std.unittest.DataStrategyProcessor<U>, _: std.core.String, x!: Int64 = COMPILED_CODE /* compiled code */): std.unittest.DataStrategyProcessor<U> { /* compiled code */ }

    DataStrategyProcessor() { /* compiled code */ }
}

internal interface Executor {
    public open func execute(suiteInfo: std.unittest.TestSuiteReportInfo, caseResult: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit
}

public interface Measurement {
    public open func setup(): Unit { /* compiled code */ }

    @Frozen
    public open func measure(): Float64 { /* compiled code */ }

    public open prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    public open prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public open prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }

    public open prop info: std.unittest.MeasurementInfo {
        get() { /* compiled code */ }
    }
}

public struct TestCaseInfo {
    public let groupName: std.core.String

    public let suiteName: std.core.String

    public let caseName: std.core.String

    TestCaseInfo(groupName: std.core.String, suiteName: std.core.String, caseName: std.core.String) { /* compiled code */ }
}

public abstract open class Report {
    private let details: std.unittest.Details

    public prop errorCount: Int64 {
        get() { /* compiled code */ }
    }

    public prop caseCount: Int64 {
        get() { /* compiled code */ }
    }

    public prop passedCount: Int64 {
        get() { /* compiled code */ }
    }

    public prop failedCount: Int64 {
        get() { /* compiled code */ }
    }

    public prop skippedCount: Int64 {
        get() { /* compiled code */ }
    }

    internal let groupResult: std.unittest.TestGroupResult

    internal let runConfiguration: std.unittest.common.Configuration

    Report(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

public open interface Reporter<TReport, TReturn> {
}

public interface TestClass {
    public open func asTestSuite(): std.unittest.TestSuite
}

public class TestPackage {
    private let builder: std.unittest.TestGroupBuilder

    public func registerCase(testCase: () -> std.unittest.UnitTestCase): Unit { /* compiled code */ }

    public func registerSuite(suite: () -> std.unittest.TestSuite): Unit { /* compiled code */ }

    public func registerBench(bench: () -> std.unittest.Benchmark): Unit { /* compiled code */ }

    internal func build(): std.unittest.TestGroup { /* compiled code */ }

    internal let name: std.core.String

    TestPackage(name: std.core.String) { /* compiled code */ }
}

internal interface FrameworkTestFilter <: std.unittest.TestFilter {
    public open func register(_: std.unittest.SuiteFilterKey, _: std.unittest.TestSuite): Unit { /* compiled code */ }
}

extend std.unittest.TestGroupResult {
    internal func asBenchReport(runConfiguration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    internal func asTestReport(runConfiguration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }
}

public func assertEqual<T>(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public class AssertionCtx {
    private let _stringArgs: std.core.String

    private var _argsMap: std.core.Option<std.collection.HashMap<std.core.String, std.core.String>>

    internal let _errors: std.collection.ArrayList<std.unittest.PrettyTreeStruct>

    public prop caller: std.core.String {
        get() { /* compiled code */ }
    }

    public prop hasErrors: Bool {
        get() { /* compiled code */ }
    }

    public func arg(key: std.core.String): std.core.String { /* compiled code */ }

    public prop args: std.core.String {
        get() { /* compiled code */ }
    }

    public func setArgsAliases(aliases: std.core.Array<std.core.String>): Unit { /* compiled code */ }

    public func fail(message: std.core.String): Nothing { /* compiled code */ }

    public func fail<PP>(pt: PP): Nothing { /* compiled code */ }

    public func failExpect(message: std.core.String): Unit { /* compiled code */ }

    public func failExpect<PP>(pt: PP): Unit { /* compiled code */ }

    private let _passedArgs: std.core.Array<std.core.String>

    private let _caller: std.core.String

    AssertionCtx(_passedArgs!: std.core.Array<std.core.String> = COMPILED_CODE /* compiled code */, _caller!: std.core.String = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

public func assertEqual<T>(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta!: Bool, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func expectEqual<T>(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public func expectEqual<T>(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta!: Bool, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal func buildCheckResult<T>(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, checkResultKind: std.unittest.CheckResultKind, isDelta!: Bool = COMPILED_CODE /* compiled code */): std.unittest.AssertExpectCheckResult { /* compiled code */ }

internal enum CheckResultKind <: std.unittest.common.Serializable<std.unittest.CheckResultKind> {
    | Assert

    | Expect

    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResultKind { /* compiled code */ }
}

internal abstract class AssertExpectCheckResult <: std.unittest.CheckResult {
    internal let kind: std.unittest.CheckResultKind

    AssertExpectCheckResult(kind: std.unittest.CheckResultKind) { /* compiled code */ }
}

public class AssertException <: std.core.Exception {
    internal let cause: std.core.Option<std.unittest.CheckResult>

    init() { /* compiled code */ }

    init(message: std.core.String, cause: std.unittest.CheckResult) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal abstract class CheckResult <: std.core.Object & std.unittest.common.Serializable<std.unittest.CheckResult> {
    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResult { /* compiled code */ }

    init() { /* compiled code */ }
}

public class AssertIntermediateException <: std.core.Exception {
    internal let diagram: std.unittest.PowerAssertDiagram

    public let expression: std.core.String

    public let originalException: std.core.Exception

    init(exception: std.core.Exception, expression: std.core.String, message: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }

    public func getOriginalStackTrace(): std.core.String { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

internal class PowerAssertDiagram <: std.core.Object & std.core.ToString & std.unittest.common.Serializable<std.unittest.PowerAssertDiagram> & std.unittest.common.PrettyPrintable {
    private let diagram: std.collection.ArrayList<std.collection.ArrayList<std.unittest.PowerAssertValue>>

    private var offset: Int64

    init() { /* compiled code */ }

    internal func setOffset(offset: Int64): Unit { /* compiled code */ }

    internal func addValue(newValue: std.unittest.PowerAssertValue): Unit { /* compiled code */ }

    public func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertDiagram { /* compiled code */ }

    internal func pprintExpression(expr: std.core.String, pp: std.unittest.common.PrettyPrinter): std.core.Any { /* compiled code */ }
}

public interface BenchmarkInputMarker {
}

internal struct SimpleBenchWrapper<T> <: std.unittest.BenchmarkWrapper {
    public func measureLoopOnce(times: Int64, max: Int64): Float64 { /* compiled code */ }

    public func timeLoopOnce(times: Int64, max: Int64): Float64 { /* compiled code */ }

    internal let input: T

    internal let f: (T, Int64, Int64) -> Float64

    SimpleBenchWrapper(input: T, f: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

internal interface BenchmarkWrapper {
    public open func measureLoopOnce(times: Int64, max: Int64): Float64

    public open func timeLoopOnce(times: Int64, max: Int64): Float64
}

public interface BenchmarkConfig {
    public open func batchSize(b: Int64): Unit

    public open func batchSize(x: std.core.Range<Int64>): Unit

    public open func warmup(x: Int64): Unit

    public open func warmup(x: std.core.Duration): Unit

    public open func minDuration(x: std.core.Duration): Unit

    public open func explicitGC(x: std.unittest.ExplicitGcType): Unit

    public open func minBatches(x: Int64): Unit
}

public enum ExplicitGcType <: std.core.ToString {
    | Disabled

    | Light

    | Heavy

    internal func invoke(): Unit { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.common.Configuration <: BenchmarkConfig {
    public func batchSize(b: Int64): Unit { /* compiled code */ }

    public func batchSize(x: std.core.Range<Int64>): Unit { /* compiled code */ }

    public func warmup(x: Int64): Unit { /* compiled code */ }

    public func warmup(x: std.core.Duration): Unit { /* compiled code */ }

    public func minDuration(x: std.core.Duration): Unit { /* compiled code */ }

    public func explicitGC(x: std.unittest.ExplicitGcType): Unit { /* compiled code */ }

    public func minBatches(x: Int64): Unit { /* compiled code */ }
}

internal type BenchRawMeasurement = (Float64, Float64)

internal class BenchExecutor<T> <: std.core.Object & std.unittest.Executor {
    internal var step: Int64 = COMPILED_CODE /* compiled code */

    public func execute(suiteInfo: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    internal func runSingle(value: T, inputs: std.core.Array<std.unittest.InputParameter>, suiteInfo: std.unittest.TestSuiteReportInfo, caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, benchRunner: std.unittest.BenchRunner, progress: std.unittest.ProgressOutput, configuration: std.unittest.common.Configuration, step: Int64): std.unittest.RunStepResult { /* compiled code */ }

    private let strategy: std.unittest.DataStrategyProcessor<T>

    private let doRun: (T, Int64, Int64) -> Float64

    BenchExecutor(strategy: std.unittest.DataStrategyProcessor<T>, doRun: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

internal struct TestSuiteReportInfo {
    internal prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    internal let tags: std.core.Array<std.core.String>

    TestSuiteReportInfo(tags!: std.core.Array<std.core.String>) { /* compiled code */ }
}

public class InputParameter {
    internal var position: Int64

    internal var repr: std.core.String

    internal func update<T>(param: T): Unit { /* compiled code */ }

    internal let name: std.core.String

    InputParameter(name: std.core.String) { /* compiled code */ }
}

internal struct TestCaseId <: std.unittest.common.Serializable<std.unittest.TestCaseId> & std.core.Hashable & std.core.Equatable<std.unittest.TestCaseId> & std.core.ToString {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseId { /* compiled code */ }

    public operator func ==(other: std.unittest.TestCaseId): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.TestCaseId): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func fqName(): std.core.String { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal let suiteId: std.unittest.TestSuiteId

    internal let caseName: std.core.String

    internal let isBench: Bool

    TestCaseId(suiteId: std.unittest.TestSuiteId, caseName: std.core.String, isBench!: Bool) { /* compiled code */ }
}

internal struct TestCaseReportInfo {
    internal static func empty(): std.unittest.TestCaseReportInfo { /* compiled code */ }

    internal prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    internal func merge(suiteInfo: std.unittest.TestSuiteReportInfo): std.unittest.TestCaseReportInfo { /* compiled code */ }

    internal let tags: std.core.Array<std.core.String>

    TestCaseReportInfo(tags!: std.core.Array<std.core.String>) { /* compiled code */ }
}

internal class BenchRunner {
    internal var benchmark: std.unittest.BenchmarkWrapper

    internal var measurements: std.collection.ArrayList<(Float64, Float64)>

    private let time: std.unittest.TimeNow

    private var explicitGC: std.unittest.ExplicitGcType

    private var targetDuration: std.core.Duration

    private var minBatches: Int64

    private var batchSize: std.core.Range<Int64>

    private var overheadPerIteration: Float64

    private var overheadPerBatch: Float64

    private var random: std.random.Random

    internal var isRunning: Bool

    private var ema0: std.unittest.Ema

    private var ema1: std.unittest.Ema

    internal func runBench(): Unit { /* compiled code */ }

    internal func runBenchInner(): Unit { /* compiled code */ }

    internal func calcBatchSize(estimation: Float64): (std.core.Range<Int64>, Int64) { /* compiled code */ }

    internal let configuration: std.unittest.common.Configuration

    internal let progress: std.unittest.ProgressOutput

    BenchRunner(configuration: std.unittest.common.Configuration, progress: std.unittest.ProgressOutput) { /* compiled code */ }
}

internal class ProgressOutput {
    private let isVerbose: Bool

    internal func println(message: () -> std.core.String): Unit { /* compiled code */ }

    internal func verboseLine(message: std.core.String): Unit { /* compiled code */ }

    internal func verbose(message: std.core.String): Unit { /* compiled code */ }

    ProgressOutput(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

internal struct StatisticsSample {
    internal let _kde: std.collection.ArrayList<(Float64, Float64)>

    init() { /* compiled code */ }

    internal func append(data: Float64): Unit { /* compiled code */ }

    internal func finish(): Unit { /* compiled code */ }

    internal prop mean: Float64 {
        get() { /* compiled code */ }
    }

    internal prop median: Float64 {
        get() { /* compiled code */ }
    }

    internal prop lowerBound: Float64 {
        get() { /* compiled code */ }
    }

    internal prop upperBound: Float64 {
        get() { /* compiled code */ }
    }

    internal prop kde: std.collection.ArrayList<(Float64, Float64)> {
        get() { /* compiled code */ }
    }

    internal prop mode: Float64 {
        get() { /* compiled code */ }
    }

    internal func mode2(): Float64 { /* compiled code */ }

    internal let sample: std.unittest.Sample

    StatisticsSample(sample: std.unittest.Sample) { /* compiled code */ }
}

internal class Sample {
    internal prop size: Int64 {
        get() { /* compiled code */ }
    }

    internal func mean(): Float64 { /* compiled code */ }

    internal func percentile(ratio: Float64): Float64 { /* compiled code */ }

    internal static func create(data: std.collection.ArrayList<Float64>): std.unittest.Sample { /* compiled code */ }

    internal func kde(kdeThroughput: Float64): std.collection.ArrayList<(Float64, Float64)> { /* compiled code */ }

    internal let data: std.collection.ArrayList<Float64>

    Sample(data: std.collection.ArrayList<Float64>) { /* compiled code */ }
}

let BOOTSTRAP_ITERATIONS: Int64

internal class BenchmarkResult {
    internal var finished: Bool

    internal var bootstrapped: std.unittest.BootstrapResult

    internal prop median: Float64 {
        get() { /* compiled code */ }
    }

    internal prop medianCI99: Float64 {
        get() { /* compiled code */ }
    }

    internal prop medianOverhead: Float64 {
        get() { /* compiled code */ }
    }

    internal prop mean: Float64 {
        get() { /* compiled code */ }
    }

    internal prop meanCI99: Float64 {
        get() { /* compiled code */ }
    }

    internal prop meanLower: Float64 {
        get() { /* compiled code */ }
    }

    internal prop meanUpper: Float64 {
        get() { /* compiled code */ }
    }

    internal prop meanOverhead: Float64 {
        get() { /* compiled code */ }
    }

    internal prop mainResult: Float64 {
        get() { /* compiled code */ }
    }

    internal prop errorEst: Float64 {
        get() { /* compiled code */ }
    }

    internal func calculate(): Unit { /* compiled code */ }

    internal func valuesSample(): std.unittest.StatisticsSample { /* compiled code */ }

    internal var data: std.collection.ArrayList<(Float64, Float64)>

    BenchmarkResult(data: std.collection.ArrayList<(Float64, Float64)>) { /* compiled code */ }
}

internal struct BootstrapResult {
    internal let meanStats: std.unittest.StatisticsSample

    internal let meanOverheadStats: std.unittest.StatisticsSample

    internal let medianStats: std.unittest.StatisticsSample

    internal let medianOverheadStats: std.unittest.StatisticsSample

    internal let r2Stats: std.unittest.StatisticsSample

    internal let buffer: std.collection.ArrayList<Float64>

    internal func join(other: std.unittest.BootstrapResult): std.unittest.BootstrapResult { /* compiled code */ }

    internal func finish(): Unit { /* compiled code */ }

    internal func appendStatisticsFromSample(subsample: std.collection.ArrayList<(Float64, Float64)>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func percentToColor(p: Float64): std.unittest.common.Color { /* compiled code */ }

let PLUS_MINUS: std.core.String

private enum Baseline {
    | PreviousReport

    | TestCaseBaseline(Float64)

    | NotFound
}

private class BenchTableProperties {
    internal var hasArgsColumn: Bool

    internal var hasCasesBaseline: Bool

    internal var hasMeasurementColumn: Bool

    internal var hasRatioColumn: Bool

    internal var shouldPrintMeasurement: Bool

    internal var conversionTable: std.core.Array<(Float64, std.core.String)>

    internal var measurementName: std.core.String

    internal var baseline: std.unittest.Baseline

    protected let baselineReport: std.collection.HashMap<std.core.String, Float64>

    BenchTableProperties(hasArgsColumn!: Bool = COMPILED_CODE /* compiled code */, hasCasesBaseline!: Bool = COMPILED_CODE /* compiled code */, hasMeasurementColumn!: Bool = COMPILED_CODE /* compiled code */, hasRatioColumn!: Bool = COMPILED_CODE /* compiled code */, shouldPrintMeasurement!: Bool = COMPILED_CODE /* compiled code */, conversionTable!: std.core.Array<(Float64, std.core.String)> = COMPILED_CODE /* compiled code */, measurementName!: std.core.String = COMPILED_CODE /* compiled code */, baseline!: std.unittest.Baseline = COMPILED_CODE /* compiled code */, baselineReport!: std.collection.HashMap<std.core.String, Float64> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class PrettyTable <: std.core.Object & std.unittest.common.PrettyPrintable {
    internal let columnSizes: std.collection.ArrayList<Int64>

    internal let rows: std.collection.ArrayList<std.collection.ArrayList<(std.core.String, std.unittest.common.Color)>>

    internal var column: Int64

    internal let leftAlignedColumns: std.collection.HashSet<Int64>

    init() { /* compiled code */ }

    internal func addColumns<T>(names: T): Unit { /* compiled code */ }

    internal func addColumn(name: std.core.String): Unit { /* compiled code */ }

    internal func removeLastColumn(): (std.core.String, std.unittest.common.Color) { /* compiled code */ }

    internal func alignColumnLeft(idx: Int64): Bool { /* compiled code */ }

    internal func addEmptyRow(): Unit { /* compiled code */ }

    internal func nextRow(): Unit { /* compiled code */ }

    internal func removeCurrentRow(): std.collection.ArrayList<(std.core.String, std.unittest.common.Color)> { /* compiled code */ }

    internal func addCell(value: std.core.String, color!: std.unittest.common.Color = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    public func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    internal let names: std.collection.ArrayList<std.core.String>

    PrettyTable(names: std.collection.ArrayList<std.core.String>) { /* compiled code */ }
}

public struct TimeNow <: std.unittest.Measurement {
    internal var unit: std.core.Option<std.unittest.TimeUnit>

    init(unit: std.core.Option<std.unittest.TimeUnit>) { /* compiled code */ }

    init() { /* compiled code */ }

    @Frozen
    public func measure(): Float64 { /* compiled code */ }

    public prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }
}

internal struct Ema {
    internal var result: Float64

    private var currentLength: Float64

    internal mut func reset(): Unit { /* compiled code */ }

    internal func reachedMax(): Bool { /* compiled code */ }

    internal mut func append(next: Float64): Unit { /* compiled code */ }

    internal let maxLength: Int64

    Ema(maxLength: Int64) { /* compiled code */ }
}

public type MeasurementUnitTable = std.core.Array<(Float64, std.core.String)>

let RADIX_UNIT_TABLE: std.core.Array<(Float64, std.core.String)>

internal func printDuration(dur: std.core.Duration): std.core.String { /* compiled code */ }

public enum TimeUnit <: std.core.ToString {
    | Nanos

    | Micros

    | Millis

    | Seconds

    public func toString(): std.core.String { /* compiled code */ }

    internal func inNanos(): Float64 { /* compiled code */ }

    internal func conversionRow(): (Float64, std.core.String) { /* compiled code */ }
}

internal foreign func GetRdtsc(): UInt64

public struct CpuCycles <: std.unittest.Measurement {
    @Frozen
    public func measure(): Float64 { /* compiled code */ }

    public func setup(): Unit { /* compiled code */ }

    public prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal class UnattachedCheckResult <: std.unittest.CheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.UnattachedCheckResult> { /* compiled code */ }

    internal let inner: std.unittest.CheckResult

    UnattachedCheckResult(inner: std.unittest.CheckResult) { /* compiled code */ }
}

internal abstract class ErrorCheckResult <: std.unittest.CheckResult {
    init() { /* compiled code */ }
}

internal class PlainCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainCompareCheckResult> { /* compiled code */ }

    internal let leftExpr: std.core.String

    internal let rightExpr: std.core.String

    internal let leftValue: std.core.String

    internal let rightValue: std.core.String

    PlainCompareCheckResult(kind: std.unittest.CheckResultKind, leftExpr: std.core.String, rightExpr: std.core.String, leftValue: std.core.String, rightValue: std.core.String) { /* compiled code */ }
}

internal class PlainDeltaCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainDeltaCompareCheckResult> { /* compiled code */ }

    internal let expr: std.core.String

    PlainDeltaCompareCheckResult(kind: std.unittest.CheckResultKind, expr: std.core.String) { /* compiled code */ }
}

internal class FailCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.FailCheckResult> { /* compiled code */ }

    internal let failMessage: std.core.String

    FailCheckResult(kind: std.unittest.CheckResultKind, failMessage: std.core.String) { /* compiled code */ }
}

internal class AssertThrowsCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.AssertThrowsCheckResult> { /* compiled code */ }

    internal let message: std.core.String

    internal let expectedExceptions: std.core.String

    internal let caughtException: std.core.String

    AssertThrowsCheckResult(kind: std.unittest.CheckResultKind, message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String) { /* compiled code */ }
}

internal class PowerAssertCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertCheckResult> { /* compiled code */ }

    internal let expr: std.core.String

    internal let diagram: std.unittest.PowerAssertDiagram

    PowerAssertCheckResult(expr: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

internal class PowerAssertExceptionCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertExceptionCheckResult> { /* compiled code */ }

    internal let expression: std.core.String

    internal let exception: std.core.String

    internal let diagram: std.unittest.PowerAssertDiagram

    PowerAssertExceptionCheckResult(expression: std.core.String, exception: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

internal class DiffCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult> { /* compiled code */ }

    internal let diffMessage: std.unittest.common.PrettyText

    DiffCheckResult(kind: std.unittest.CheckResultKind, diffMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

internal class CustomCheckResult <: std.unittest.AssertExpectCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult> { /* compiled code */ }

    internal let custom: std.unittest.common.PrettyText

    CustomCheckResult(kind: std.unittest.CheckResultKind, custom: std.unittest.common.PrettyText) { /* compiled code */ }
}

internal class ExceptionThrownCheckResult <: std.unittest.ErrorCheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.ExceptionThrownCheckResult> { /* compiled code */ }

    internal let errorMessage: std.unittest.common.PrettyText

    internal let stackTrace: std.unittest.common.PrettyText

    ExceptionThrownCheckResult(errorMessage: std.unittest.common.PrettyText, stackTrace: std.unittest.common.PrettyText) { /* compiled code */ }
}

internal class MockFrameworkCheckResult <: std.unittest.CheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.MockFrameworkCheckResult> { /* compiled code */ }

    internal let mockFrameworkMessage: std.unittest.common.PrettyText

    MockFrameworkCheckResult(mockFrameworkMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

internal class NoParametersProvidedResult <: std.unittest.CheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    internal static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.NoParametersProvidedResult> { /* compiled code */ }

    NoParametersProvidedResult() { /* compiled code */ }
}

internal class CrashedCheckResult <: std.unittest.CheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    internal static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.CrashedCheckResult> { /* compiled code */ }

    internal let exitCode: Int64

    CrashedCheckResult(exitCode: Int64) { /* compiled code */ }
}

internal class TimeoutCheckResult <: std.unittest.CheckResult {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    internal static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.TimeoutCheckResult> { /* compiled code */ }

    internal let msg: std.unittest.common.PrettyText

    TimeoutCheckResult(msg: std.unittest.common.PrettyText) { /* compiled code */ }
}

internal enum PrettyTreeStruct <: std.unittest.common.PrettyPrintable {
    | Leaf(std.unittest.AssertExpectCheckResult)

    | Node(std.core.String, std.collection.ArrayList<std.unittest.PrettyTreeStruct>)

    public func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

public func invokeCustomAssert<T>(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, assert: (std.unittest.AssertionCtx) -> T, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): T { /* compiled code */ }

public func invokeCustomExpect(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, expect: (std.unittest.AssertionCtx) -> std.core.Any, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

internal func fillAbsentValues(config: std.unittest.common.Configuration): Unit { /* compiled code */ }

internal func kebabCaseToCamelCase(str: std.core.String): std.core.String { /* compiled code */ }

internal func camelCaseToKebabCase(str: std.core.String): std.core.String { /* compiled code */ }

internal func setValueWithGuessedType(configuration: std.unittest.common.Configuration, key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

internal func buildDefaultConfiguration(): std.unittest.common.Configuration { /* compiled code */ }

var defaultConfigurationCached: std.sync.AtomicOptionReference<std.unittest.common.Configuration>

internal func setValuesFromJsonConfiguration(configuration: std.unittest.common.Configuration, rawJson: std.core.String): Unit { /* compiled code */ }

internal func typeValidator<T>(name: std.core.String): std.unittest.common.OptionValidity { /* compiled code */ }

internal func registerUnittestOption<T>(key: std.unittest.common.KeyFor<T>, description!: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */, typeDescription!: std.core.Option<(std.core.String, std.core.String)> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

public interface NearEquatable<CT, D> {
    public open func isNear(obj: CT, delta!: D): Bool
}

public struct RelativeDelta<T> {
    internal let absolute: T

    internal let relative: T

    RelativeDelta(absolute!: T, relative!: T) { /* compiled code */ }
}

public func isNearExpansion<CT, D>(l: CT, r: CT, delta!: D, cmpType!: std.core.String): Bool { /* compiled code */ }

public func isNearExpansion<CT, D>(l: CT, r: CT, delta!: D, cmpType!: std.core.String, overloadHack!: Bool = COMPILED_CODE /* compiled code */): Bool { /* compiled code */ }

extend Float16 <: NearEquatable & NearEquatable {
    public func isNear(obj: Float16, delta!: Float16): Bool { /* compiled code */ }

    public func isNear(obj: Float16, delta!: std.unittest.RelativeDelta<Float16>): Bool { /* compiled code */ }
}

extend Float32 <: NearEquatable & NearEquatable {
    public func isNear(obj: Float32, delta!: Float32): Bool { /* compiled code */ }

    public func isNear(obj: Float32, delta!: std.unittest.RelativeDelta<Float32>): Bool { /* compiled code */ }
}

extend Float64 <: NearEquatable & NearEquatable {
    public func isNear(obj: Float64, delta!: Float64): Bool { /* compiled code */ }

    public func isNear(obj: Float64, delta!: std.unittest.RelativeDelta<Float64>): Bool { /* compiled code */ }
}

public func entryMain(testPackage: std.unittest.TestPackage): Int64 { /* compiled code */ }

protected func testRunnerEntryMain(): Int64 { /* compiled code */ }

internal class ModuleExecutionResult <: std.unittest.ResultContainer<std.unittest.TestGroupResult> {
    internal let moduleName: std.core.String

    ModuleExecutionResult(moduleName: std.core.String) { /* compiled code */ }
}

internal abstract class ResultContainer<T> <: std.core.Object & std.unittest.Result {
    internal let subResults: std.collection.ArrayList<T> = COMPILED_CODE /* compiled code */

    public func visitChildren(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    public open func add(child: T): Unit { /* compiled code */ }

    protected var _details: std.unittest.Details = COMPILED_CODE /* compiled code */

    private var _detailsComputed: Bool = COMPILED_CODE /* compiled code */

    private var finished: Bool = COMPILED_CODE /* compiled code */

    internal prop isFinished: Bool {
        get() { /* compiled code */ }
    }

    public prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    internal func finish(): Unit { /* compiled code */ }

    internal func visitAll<V>(visitor: (V) -> Unit): Unit { /* compiled code */ }

    public func visitPassedTests(visitor: (std.unittest.TestCaseResult) -> Unit): Unit { /* compiled code */ }

    public func visitPassedBenches(visitor: (std.unittest.BenchStepEntry) -> Unit): Unit { /* compiled code */ }

    public func visitFailedSteps(visitor: (std.unittest.RunStepResult, std.unittest.TestCaseResult) -> Unit): Unit { /* compiled code */ }

    internal func serializeSubResults(): std.unittest.common.DataModelStruct { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class ProjectExecutionResult <: std.unittest.ResultContainer<std.unittest.ModuleExecutionResult> {
    init() { /* compiled code */ }
}

internal class PackageExecutionResult {
    internal let packageResult: std.unittest.TestGroupResult

    PackageExecutionResult(packageResult: std.unittest.TestGroupResult) { /* compiled code */ }
}

internal struct TestPackageExecuteCommand <: std.unittest.common.Serializable<std.unittest.TestPackageExecuteCommand> {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    internal func overridingCurrentEnv(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    internal func withJsonConfiguration(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    internal func withRunnerOption(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    internal func withArgs(newArgs: std.core.Array<std.core.String>): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    internal static func fromCurrentProcess(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    public let command: std.core.String

    public let args: std.core.Array<std.core.String>

    public let env: std.collection.Map<std.core.String, std.core.String>

    TestPackageExecuteCommand(command: std.core.String, args: std.core.Array<std.core.String>, env: std.collection.Map<std.core.String, std.core.String>) { /* compiled code */ }
}

internal struct ParallelCtx {
    internal let nWorkersPerPackage: Int64

    internal let workersQuota: std.sync.Semaphore

    ParallelCtx(nWorkersPerPackage: Int64, workersQuota: std.sync.Semaphore) { /* compiled code */ }
}

internal struct ReportCtx {
    internal let perPackageReporter: std.unittest.PerPackageReporter

    internal let outputReporter: std.unittest.TestOutputReporter

    ReportCtx(perPackageReporter: std.unittest.PerPackageReporter, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

internal class PerPackageReporter {
    internal func printIntermediateResult(executionResult: std.unittest.PackageExecutionResult): Unit { /* compiled code */ }

    internal func dumpPackageReport(executionResult: std.unittest.PackageExecutionResult): std.core.Option<Unit> { /* compiled code */ }

    private let format: std.core.Option<std.unittest.ReportFormat>

    private let outputReporter: std.unittest.TestOutputReporter

    PerPackageReporter(format: std.core.Option<std.unittest.ReportFormat>, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

internal class TestOutputReporter {
    internal func report(pp: std.unittest.common.PrettyPrinter, prefix: std.unittest.common.PrettyText, output: std.unittest.CapturedOutput, success!: Bool): std.unittest.common.PrettyPrinter { /* compiled code */ }

    internal static func fromConfiguration(conf: std.unittest.common.Configuration): std.unittest.TestOutputReporter { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.unittest.TestOutputReporter { /* compiled code */ }

    internal let capture: Bool

    internal let showAllOutput: Bool

    internal let verbose: Bool
}

let EXIT_CODE_ON_TIMEOUT: Int64

internal struct MainExecutionCtx {
    internal func withCommand(command: std.unittest.TestPackageExecuteCommand): std.unittest.MainExecutionCtx { /* compiled code */ }

    internal let nWorkers: Int64

    internal let workersQuota: std.sync.Semaphore

    internal let outputReporter: std.unittest.TestOutputReporter

    internal let executeCommand: std.unittest.TestPackageExecuteCommand

    MainExecutionCtx(nWorkers: Int64, workersQuota: std.sync.Semaphore, outputReporter: std.unittest.TestOutputReporter, executeCommand: std.unittest.TestPackageExecuteCommand) { /* compiled code */ }
}

internal func executeSmart(packageName: std.core.String, tests: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService, outputReporter: std.unittest.TestOutputReporter, needStartWorker!: Bool): std.unittest.TestGroupResult { /* compiled code */ }

internal func initWorkersMain(ctx: std.unittest.MainExecutionCtx): std.core.Array<std.unittest.WorkerProcess> { /* compiled code */ }

internal class WorkerProcess {
    internal func waitCasesRegistration(): std.core.Option<Bool> { /* compiled code */ }

    internal func commitCasesRegistrationFailure(): std.core.Option<Bool> { /* compiled code */ }

    internal static func create(ctx: std.unittest.MainExecutionCtx, workerId: Int64, progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>, connect: () -> std.unittest.TransportChannel): std.unittest.WorkerProcess { /* compiled code */ }

    internal static func createAndRegister(workerId: Int64, ctx: std.unittest.MainExecutionCtx): std.unittest.WorkerProcess { /* compiled code */ }

    internal static func executeRestarting(initialWorker: std.unittest.WorkerProcess, ctx: std.unittest.MainExecutionCtx): std.unittest.ExecuteRestartingResult { /* compiled code */ }

    private let process: std.unittest.WrappedProcess

    private let transportChannel: std.core.Option<std.unittest.TransportChannel>

    private let progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    private let outputReporter: std.unittest.TestOutputReporter

    private let workerId: Int64
}

internal func executeMain(workers: std.core.Array<std.unittest.WorkerProcess>, packageName: std.core.String, ctx: std.unittest.MainExecutionCtx): std.unittest.TestGroupResult { /* compiled code */ }

internal func uniqueValues<T>(collection: std.core.Collection<T>): std.core.Array<T> { /* compiled code */ }

internal func executeWorker(packageName: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService): std.unittest.TestGroupResult { /* compiled code */ }

internal enum TestProcessKind {
    | Main

    | Worker(Int64, Int64, Int64, UInt16)

    internal static func fromDefaultConfiguration(): std.unittest.TestProcessKind { /* compiled code */ }

    internal prop isWorker: Bool {
        get() { /* compiled code */ }
    }
}

internal struct NonTestOutputs <: std.core.Hashable {
    public func hashCode(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }

    init(pair: (std.unittest.CapturedOutput, std.unittest.CapturedOutput)) { /* compiled code */ }

    internal func isEmpty(): Bool { /* compiled code */ }

    internal let outputBeforeTests: std.unittest.CapturedOutput

    internal let outputAfterTests: std.unittest.CapturedOutput

    NonTestOutputs(outputBeforeTests: std.unittest.CapturedOutput, outputAfterTests: std.unittest.CapturedOutput) { /* compiled code */ }
}

internal struct CapturedOutput {
    init() { /* compiled code */ }

    init(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }

    internal func isEmpty(): Bool { /* compiled code */ }

    internal let stdout: std.core.String

    internal let stderr: std.core.String

    CapturedOutput(stdout: std.core.String, stderr: std.core.String) { /* compiled code */ }
}

internal enum ExecuteRestartingResult {
    | OkResult(std.core.Collection<std.unittest.TestSuiteResult>, std.collection.ArrayList<std.unittest.NonTestOutputs>)

    | CrashResult(std.core.Collection<std.unittest.TestSuiteResult>, std.unittest.TestProcessError, std.collection.ArrayList<std.unittest.NonTestOutputs>)
}

internal class TestProcessError <: std.core.Object & std.unittest.Result & std.core.Hashable {
    internal let _details: std.unittest.Details

    public func hashCode(): Int64 { /* compiled code */ }

    public prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    internal let code: Int64

    TestProcessError(code: Int64) { /* compiled code */ }
}

internal interface UTProgress {
}

internal class TransportChannel <: std.core.Object & std.core.Resource {
    internal prop port: UInt16 {
        get() { /* compiled code */ }
    }

    public func isClosed(): Bool { /* compiled code */ }

    public func close(): Unit { /* compiled code */ }

    internal func send(part: std.unittest.ExecutionResultPart): Unit { /* compiled code */ }

    internal func receive(limit!: std.core.Option<UInt64> = COMPILED_CODE /* compiled code */): std.core.Iterator<std.unittest.ExecutionResultPart> { /* compiled code */ }

    internal func delimitOutput(): Unit { /* compiled code */ }

    private let socket: std.net.TcpSocket

    private let outputReporter: std.unittest.TestOutputReporter

    private let isMain: Bool

    TransportChannel(socket: std.net.TcpSocket, outputReporter: std.unittest.TestOutputReporter, isMain!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct WrappedProcess {
    private let status: std.sync.AtomicOptionReference<std.unittest.CapturedProcessResult>

    internal func wait(): Int64 { /* compiled code */ }

    internal func terminate(): Unit { /* compiled code */ }

    internal func stdout(): std.core.Array<UInt8> { /* compiled code */ }

    internal func stderr(): std.core.Array<UInt8> { /* compiled code */ }

    private let process: std.process.SubProcess

    private let stdOutFile: std.core.Option<std.fs.Path>

    private let stdErrFile: std.core.Option<std.fs.Path>

    WrappedProcess(process: std.process.SubProcess, stdOutFile: std.core.Option<std.fs.Path>, stdErrFile: std.core.Option<std.fs.Path>) { /* compiled code */ }
}

internal abstract class ExecutionResultPart <: std.core.Object & std.unittest.common.Serializable<std.unittest.ExecutionResultPart> {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.ExecutionResultPart { /* compiled code */ }

    init() { /* compiled code */ }
}

internal abstract class ExecutionResultHeader <: std.unittest.ExecutionResultPart {
    public prop suiteId: std.unittest.TestSuiteId

    public prop suiteInfo: std.unittest.TestSuiteReportInfo

    public prop startTime: std.time.DateTime

    init() { /* compiled code */ }
}

internal struct TestSuiteId <: std.unittest.common.Serializable<std.unittest.TestSuiteId> & std.core.Hashable & std.core.Equatable<std.unittest.TestSuiteId> & std.core.ToString {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteId { /* compiled code */ }

    public operator func ==(other: std.unittest.TestSuiteId): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.TestSuiteId): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    public static func fromTestSuite(groupName: std.core.String, testSuite: std.unittest.TestSuite): std.unittest.TestSuiteId { /* compiled code */ }

    internal prop isFromTopLevelFunc: Bool {
        get() { /* compiled code */ }
    }

    internal let groupName: std.core.String

    internal let suiteName: std.core.String

    TestSuiteId(groupName: std.core.String, suiteName: std.core.String) { /* compiled code */ }
}

internal class TestCaseExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    public override prop suiteId: std.unittest.TestSuiteId {
        get() { /* compiled code */ }
    }

    public override prop suiteInfo: std.unittest.TestSuiteReportInfo {
        get() { /* compiled code */ }
    }

    public override prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    internal let caseId: std.unittest.TestCaseId

    internal let caseInfo: std.unittest.TestCaseReportInfo

    private let suiteInfo_: std.unittest.TestSuiteReportInfo

    private let startTime_: std.time.DateTime

    TestCaseExecutionResultHeader(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo_: std.unittest.TestSuiteReportInfo, startTime_: std.time.DateTime) { /* compiled code */ }
}

internal class LifecycleExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    public override prop suiteId: std.unittest.TestSuiteId {
        get() { /* compiled code */ }
    }

    public override prop suiteInfo: std.unittest.TestSuiteReportInfo {
        get() { /* compiled code */ }
    }

    public override prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    private let suiteId_: std.unittest.TestSuiteId

    private let suiteInfo_: std.unittest.TestSuiteReportInfo

    internal let step: std.unittest.LStep

    private let startTime_: std.time.DateTime

    LifecycleExecutionResultHeader(suiteId_: std.unittest.TestSuiteId, suiteInfo_: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep, startTime_: std.time.DateTime) { /* compiled code */ }
}

internal enum LStep <: std.core.ToString {
    | BeforeEach

    | BeforeAll

    | AfterAll

    | AfterEach

    internal operator func ==(other: std.unittest.LStep): Bool { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal class ExecutionResultBody <: std.unittest.ExecutionResultPart {
    internal let stepResult: std.unittest.TestCaseResult

    ExecutionResultBody(stepResult: std.unittest.TestCaseResult) { /* compiled code */ }
}

internal class TestCasesRegistrationPart <: std.unittest.ExecutionResultPart {
    internal let casesCount: UInt64

    internal let workerId: Int64

    internal let packageName: std.core.String

    TestCasesRegistrationPart(casesCount: UInt64, workerId!: Int64, packageName!: std.core.String) { /* compiled code */ }
}

internal class TestCasesRegistrationFailed <: std.unittest.ExecutionResultPart {
    TestCasesRegistrationFailed() { /* compiled code */ }
}

internal class ExecutionPermitPart <: std.unittest.ExecutionResultPart {
    ExecutionPermitPart() { /* compiled code */ }
}

public func failExpect(message: std.core.String): Unit { /* compiled code */ }

public func fail(message: std.core.String): Nothing { /* compiled code */ }

public func assertCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Nothing { /* compiled code */ }

public func expectCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx!: std.core.Option<std.unittest.AssertionCtx> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

let SYM_DOT: std.core.String

let SYM_COMMA: std.core.String

let SYM_PLUS: std.core.String

let SYM_NEG: std.core.String

let OP_FILTER: std.core.String

let OP_INCLUDE_TAGS_NAME: std.core.String

let OP_INCLUDE_TAGS: std.core.String

let OP_EXCLUDE_TAGS_NAME: std.core.String

let OP_EXCLUDE_TAGS: std.core.String

let OP_EXCLUDE_TAGS_KEY: std.core.String

let OP_BENCH: std.core.String

internal struct CaseFilterKey {
    internal static func fromTestCase(suiteId: std.unittest.TestSuiteId, cob: std.unittest.CaseOrBench, config!: std.core.Option<std.unittest.common.Configuration> = COMPILED_CODE /* compiled code */): std.unittest.CaseFilterKey { /* compiled code */ }

    internal let caseId: std.unittest.TestCaseId

    internal let tags: std.core.Array<std.core.String>

    internal let isTopLevel: Bool

    CaseFilterKey(caseId: std.unittest.TestCaseId, tags!: std.core.Array<std.core.String> = COMPILED_CODE /* compiled code */, isTopLevel!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class BenchTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    public override func shouldSkipTestClass(_: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    public override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.unittest.BenchTestFilter { /* compiled code */ }

    private let benchEnabled: Bool

    BenchTestFilter(benchEnabled: Bool) { /* compiled code */ }
}

private struct FilterOption {
    public static let ACCEPTING: std.unittest.FilterOption

    public static func fromString(str: std.core.String): std.unittest.FilterOption { /* compiled code */ }

    internal prop classFilter: std.core.String {
        get() { /* compiled code */ }
    }

    internal prop funcFilter: std.core.String {
        get() { /* compiled code */ }
    }

    private let _classRegex: std.core.String

    private let _funcRegex: std.core.String

    internal let exclude: Bool

    internal let isFuncFilter: Bool

    FilterOption(_classRegex!: std.core.String = COMPILED_CODE /* compiled code */, _funcRegex!: std.core.String = COMPILED_CODE /* compiled code */, exclude!: Bool = COMPILED_CODE /* compiled code */, isFuncFilter!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

private class TagsFilterOption {
    internal static func fromString(input: std.core.String): std.unittest.TagsFilterOption { /* compiled code */ }

    internal let tags: std.collection.HashSet<std.core.String>

    TagsFilterOption(tags!: std.collection.HashSet<std.core.String> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class WorkerTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    private let shouldSkipClassCache: std.collection.HashMap<std.unittest.TestSuiteId, Bool>

    private let shouldSkipCaseCache: std.collection.HashMap<std.unittest.TestCaseId, Bool>

    private var testEntityIdx: Int64

    private var testCaseIdx: Int64

    public override func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit { /* compiled code */ }

    public override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    public override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    private let defaultFilter: std.unittest.FrameworkTestFilter

    private let workerId: Int64

    private let nWorkers: Int64

    private let nCasesSkip: Int64

    WorkerTestFilter(defaultFilter: std.unittest.FrameworkTestFilter, workerId: Int64, nWorkers: Int64, nCasesSkip: Int64) { /* compiled code */ }
}

internal enum LaunchApi {
    | FromPublicApi

    | FromCli
}

internal enum LaunchSource {
    | TestBinary

    | TestRunner

    internal static func fromDefaultConfiguration(): std.unittest.LaunchSource { /* compiled code */ }
}

internal class Framework {
    private var transport: std.core.Option<std.unittest.Transport>

    private var progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    private let mtx: std.sync.ReentrantMutex

    private var currentlyRunning: std.unittest.RunContext

    internal static func launch<T>(api!: std.unittest.LaunchApi, hasWorkersInSetup!: Bool, progressQueue!: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>> = COMPILED_CODE /* compiled code */, body!: () -> T): T { /* compiled code */ }

    internal static func isExecuting(): Bool { /* compiled code */ }

    internal static func runStepBody(stepKind: std.unittest.StepKind, info: std.unittest.StepInfo, body: () -> Unit): std.unittest.RunStepResult { /* compiled code */ }

    internal static func runCatching(f: () -> Unit): Unit { /* compiled code */ }

    internal static func withCurrentContext<T>(f: (std.unittest.RunContext) -> T): T { /* compiled code */ }

    internal static func abortCurrentStep(): std.core.Option<std.unittest.RunStepResult> { /* compiled code */ }

    internal static func collectUnattachedFailures(): std.collection.ArrayList<std.unittest.CheckResult> { /* compiled code */ }

    internal static func onTestCasesRegistered(packageName: std.core.String, casesCount: UInt64): Unit { /* compiled code */ }

    internal static func onLifecycleStart(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep): Unit { /* compiled code */ }

    internal static func onTestCaseStarted(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo: std.unittest.TestSuiteReportInfo): Unit { /* compiled code */ }

    internal static func onFinished(result: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    internal static func initWorker(ctx: std.unittest.MainExecutionCtx, workerId!: Int64): std.unittest.WorkerProcess { /* compiled code */ }
}

internal enum Transport <: std.core.Resource {
    | TChannel(std.unittest.TransportChannel)

    | TMain(std.unittest.TransportMain)

    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    internal prop channel: std.core.Option<std.unittest.TransportChannel> {
        get() { /* compiled code */ }
    }

    internal prop manager: std.core.Option<std.unittest.TransportMain> {
        get() { /* compiled code */ }
    }

    internal static func fromDefaultConfiguration(): std.unittest.Transport { /* compiled code */ }
}

internal abstract class RunContext {
    internal let checkResults: std.collection.ArrayList<std.unittest.CheckResult>

    private var suppressed: Bool

    internal var passedChecks: Int64

    protected open func checkFailed(c: std.unittest.CheckResult): Unit { /* compiled code */ }

    protected open func onException(e: std.core.Exception): Unit { /* compiled code */ }

    protected open func storeCheckResult(check: std.unittest.CheckResult): Unit { /* compiled code */ }

    internal func checkPassed(): Unit { /* compiled code */ }

    internal func suppressIfHasFailures(): Unit { /* compiled code */ }

    internal func hasFailures(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

internal enum StepKind {
    | Skip

    | NoRun

    | Lifecycle(std.unittest.LStep)

    | CaseStep(std.unittest.ArgumentDescription)

    | UserCode

    internal func isClassLifecycle(): Bool { /* compiled code */ }
}

internal enum StepInfo {
    | Test(Int64)

    | Bench(std.unittest.BenchmarkResult)

    | Failure(std.core.Array<std.unittest.CheckResult>)
}

internal class OutsideOfFrameworkContext <: std.unittest.RunContext {
    protected func storeCheckResult(_: std.unittest.CheckResult): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class FrameworkInternalsContext <: std.unittest.RunContext {
    init() { /* compiled code */ }
}

internal class RunStepContext <: std.unittest.RunContext {
    internal var startTime: std.time.DateTime

    protected func checkFailed(c: std.unittest.CheckResult): Unit { /* compiled code */ }

    internal func finishStep(): std.unittest.RunStepResult { /* compiled code */ }

    internal var stepKind: std.unittest.StepKind

    internal var stepInfo: std.unittest.StepInfo

    RunStepContext(stepKind: std.unittest.StepKind, stepInfo: std.unittest.StepInfo) { /* compiled code */ }
}

internal class BenchmarkStoppedError <: std.core.Exception {
    BenchmarkStoppedError() { /* compiled code */ }
}

internal struct UInt8Entry {
    internal let left: UInt8

    internal let right: UInt8

    UInt8Entry(left!: UInt8 = COMPILED_CODE /* compiled code */, right!: UInt8 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct UInt16Entry {
    internal let left: UInt16

    internal let right: UInt16

    UInt16Entry(left!: UInt16 = COMPILED_CODE /* compiled code */, right!: UInt16 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct UInt32Entry {
    internal let left: UInt32

    internal let right: UInt32

    UInt32Entry(left!: UInt32 = COMPILED_CODE /* compiled code */, right!: UInt32 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct UInt64Entry {
    internal let left: UInt64

    internal let right: UInt64

    UInt64Entry(left!: UInt64 = COMPILED_CODE /* compiled code */, right!: UInt64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct Float32Entry {
    internal let left: Float32

    internal let right: Float32

    Float32Entry(left!: Float32 = COMPILED_CODE /* compiled code */, right!: Float32 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal struct Float64Entry {
    internal let left: Float64

    internal let right: Float64

    Float64Entry(left!: Float64 = COMPILED_CODE /* compiled code */, right!: Float64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal foreign func CJ_MICROFUZZ_FLIP_BIT_FLOAT(value: Float32, bit: UInt8): Float32

internal foreign func CJ_MICROFUZZ_FLIP_BIT_DOUBLE(value: Float64, bit: UInt8): Float64

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT8(result: std.core.CPointer<std.unittest.UInt8Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT16(result: std.core.CPointer<std.unittest.UInt16Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT32(result: std.core.CPointer<std.unittest.UInt32Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT64(result: std.core.CPointer<std.unittest.UInt64Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FLOAT(result: std.core.CPointer<std.unittest.Float32Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_DOUBLE(result: std.core.CPointer<std.unittest.Float64Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT8(key: UInt8, result: std.core.CPointer<std.unittest.UInt8Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT16(key: UInt16, result: std.core.CPointer<std.unittest.UInt16Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT32(key: UInt32, result: std.core.CPointer<std.unittest.UInt32Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT64(key: UInt64, result: std.core.CPointer<std.unittest.UInt64Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_FLOAT(key: Float32, result: std.core.CPointer<std.unittest.Float32Entry>): Unit

internal foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_DOUBLE(key: Float64, result: std.core.CPointer<std.unittest.Float64Entry>): Unit

internal foreign func CJ_MICROFUZZ_ENABLE_TRACE(): Unit

internal foreign func CJ_MICROFUZZ_DISABLE_TRACE(): Unit

internal foreign func CJ_MICROFUZZ_RESET_LOCAL_COVERAGE_INFO(): Unit

internal foreign func CJ_MICROFUZZ_RESET_ALL_COVERAGE_INFO(): Unit

internal foreign func CJ_MICROFUZZ_HAS_NEW_COVERAGE(): Bool

internal foreign func CJ_MICROFUZZ_IS_INITIALIZED(): Bool

internal foreign func CJ_MICROFUZZ_GLOBAL_EDGE_COVERAGE_EST(): Float64

internal foreign func CJ_MICROFUZZ_SET_DBG_CALLBACK(callback: (CString, CString, UInt64, UInt64) -> Unit): Unit

internal func printMessage(mes: CString, t: CString, v1: UInt64, v2: UInt64): Unit { /* compiled code */ }

internal func currentCoverageEst(): Float64 { /* compiled code */ }

internal interface Torcable<T> {
    public static func randomRecentEntry(): std.core.Option<(T, T)>

    public static func randomRecentValueComparedTo(value: T): std.core.Option<T>

    public static func nextFrom(random: std.random.Random): T

    public static func suggestedFrom(random: std.random.Random): T

    public open func isZero(): Bool

    public open func bitFlip(at: UInt8): T

    public open operator func +(_: UInt8): T

    public open operator func -(_: UInt8): T
}

extend UInt8 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(UInt8, UInt8)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): UInt8 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): UInt8 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): UInt8 { /* compiled code */ }
}

extend UInt16 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(UInt16, UInt16)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): UInt16 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): UInt16 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): UInt16 { /* compiled code */ }

    public operator func +(arg: UInt8): UInt16 { /* compiled code */ }

    public operator func -(arg: UInt8): UInt16 { /* compiled code */ }
}

extend UInt32 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(UInt32, UInt32)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): UInt32 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): UInt32 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): UInt32 { /* compiled code */ }

    public operator func +(arg: UInt8): UInt32 { /* compiled code */ }

    public operator func -(arg: UInt8): UInt32 { /* compiled code */ }
}

extend UInt64 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(UInt64, UInt64)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): UInt64 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): UInt64 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): UInt64 { /* compiled code */ }

    public operator func +(arg: UInt8): UInt64 { /* compiled code */ }

    public operator func -(arg: UInt8): UInt64 { /* compiled code */ }
}

extend Float32 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(Float32, Float32)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: Float32): std.core.Option<Float32> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): Float32 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): Float32 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): Float32 { /* compiled code */ }

    public operator func +(arg: UInt8): Float32 { /* compiled code */ }

    public operator func -(arg: UInt8): Float32 { /* compiled code */ }
}

extend Float64 <: Torcable {
    public static func randomRecentEntry(): std.core.Option<(Float64, Float64)> { /* compiled code */ }

    public static func randomRecentValueComparedTo(value: Float64): std.core.Option<Float64> { /* compiled code */ }

    public static func nextFrom(random: std.random.Random): Float64 { /* compiled code */ }

    public static func suggestedFrom(random: std.random.Random): Float64 { /* compiled code */ }

    public func isZero(): Bool { /* compiled code */ }

    public func bitFlip(at: UInt8): Float64 { /* compiled code */ }

    public operator func +(arg: UInt8): Float64 { /* compiled code */ }

    public operator func -(arg: UInt8): Float64 { /* compiled code */ }
}

internal enum Mutation<T> {
    | Drop

    | Insert(T)

    | ReplaceWith(T)

    | BitFlip(UInt8)

    | Add(UInt8)

    | Sub(UInt8)

    internal func apply(at: Int64, values: std.collection.ArrayList<T>): Unit { /* compiled code */ }
}

internal class Mutator {
    internal func randomRecentValue<T>(): std.core.Option<T> { /* compiled code */ }

    internal func randomRecentCmp<T>(value: T): std.core.Option<T> { /* compiled code */ }

    internal func randomMutation<T>(temperature: UInt8, value: T): std.core.Option<std.unittest.Mutation<T>> { /* compiled code */ }

    internal let random: std.random.Random

    Mutator(random!: std.random.Random = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

private class MutatingRandomValueStream<T> {
    internal var currentIndex: Int64 = COMPILED_CODE /* compiled code */

    internal func nextUniform(random: std.random.Random): T { /* compiled code */ }

    internal func next(random: std.random.Random, temperature: UInt8, mutator: std.unittest.Mutator): T { /* compiled code */ }

    internal func reset(): std.unittest.MutatingRandomValueStream<T> { /* compiled code */ }

    internal func freeze(): std.unittest.MutatingRandomValueStream<T> { /* compiled code */ }

    internal let values: std.collection.ArrayList<T>

    MutatingRandomValueStream(values!: std.collection.ArrayList<T> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class CovRandomSource <: std.core.Object & std.unittest.prop_test.RandomSource {
    private let inner: std.random.Random

    init(seed!: UInt64, temperature!: UInt8 = COMPILED_CODE /* compiled code */, mutator!: std.unittest.Mutator = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    internal func copy(seed!: UInt64 = COMPILED_CODE /* compiled code */, temperature!: UInt8 = COMPILED_CODE /* compiled code */): std.unittest.CovRandomSource { /* compiled code */ }

    internal func freeze(): std.unittest.CovRandomSource { /* compiled code */ }

    public override func suggestBool(): Bool { /* compiled code */ }

    public override func suggestUInt8(): UInt8 { /* compiled code */ }

    public override func suggestUInt16(): UInt16 { /* compiled code */ }

    public override func suggestUInt32(): UInt32 { /* compiled code */ }

    public override func suggestUInt64(): UInt64 { /* compiled code */ }

    public override func suggestInt8(): Int8 { /* compiled code */ }

    public override func suggestInt16(): Int16 { /* compiled code */ }

    public override func suggestInt32(): Int32 { /* compiled code */ }

    public override func suggestInt64(): Int64 { /* compiled code */ }

    public override func nextFloat16(): Float16 { /* compiled code */ }

    public override func nextFloat32(): Float32 { /* compiled code */ }

    public override func nextFloat64(): Float64 { /* compiled code */ }

    public override func suggestFloat16(): Float16 { /* compiled code */ }

    public override func suggestFloat32(): Float32 { /* compiled code */ }

    public override func suggestFloat64(): Float64 { /* compiled code */ }

    private var gaussian: std.unittest.GaussianGenerator

    public override func nextGaussianFloat64(mean!: Float64 = COMPILED_CODE /* compiled code */, sigma!: Float64 = COMPILED_CODE /* compiled code */): Float64 { /* compiled code */ }

    public override func nextUInt8(): UInt8 { /* compiled code */ }

    public override func nextUInt16(): UInt16 { /* compiled code */ }

    public override func nextUInt32(): UInt32 { /* compiled code */ }

    public override func nextUInt64(): UInt64 { /* compiled code */ }

    internal let seed: UInt64

    internal let temperature: UInt8

    private let valuesInt8: std.unittest.MutatingRandomValueStream<UInt8>

    private let valuesInt16: std.unittest.MutatingRandomValueStream<UInt16>

    private let valuesInt32: std.unittest.MutatingRandomValueStream<UInt32>

    private let valuesInt64: std.unittest.MutatingRandomValueStream<UInt64>

    private let valuesFloat32: std.unittest.MutatingRandomValueStream<Float32>

    private let valuesFloat64: std.unittest.MutatingRandomValueStream<Float64>

    private let mutator: std.unittest.Mutator
}

internal class GaussianGenerator {
    internal var cached: std.core.Option<Float64>

    internal func next(random: std.unittest.prop_test.RandomSource): Float64 { /* compiled code */ }

    internal let mean: Float64

    internal let sigma: Float64

    GaussianGenerator(mean: Float64, sigma: Float64) { /* compiled code */ }
}

internal class CoverageInfo {
    internal static func newCoverageDiscovered(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func printUnittestHelpPageIfRequested(): Bool { /* compiled code */ }

public struct KeyHelp <: std.unittest.common.KeyFor<Bool> {
    public static prop help: std.unittest.KeyHelp {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyNoColor <: std.unittest.common.KeyFor<Bool> {
    public static prop noColor: std.unittest.KeyNoColor {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyFromTopLevel <: std.unittest.common.KeyFor<Bool> {
    public static prop fromTopLevel: std.unittest.KeyFromTopLevel {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyRandomSeed <: std.unittest.common.KeyFor<Int64> {
    public static prop randomSeed: std.unittest.KeyRandomSeed {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyWarmup <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Duration> {
    public static prop warmup: std.unittest.KeyWarmup {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyMinDuration <: std.unittest.common.KeyFor<std.core.Duration> {
    public static prop minDuration: std.unittest.KeyMinDuration {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyExplicitGC <: std.unittest.common.KeyFor<std.unittest.ExplicitGcType> {
    public static prop explicitGC: std.unittest.KeyExplicitGC {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyMinBatches <: std.unittest.common.KeyFor<Int64> {
    public static prop minBatches: std.unittest.KeyMinBatches {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyBatchSize <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Range<Int64>> {
    public static prop batchSize: std.unittest.KeyBatchSize {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyParallel <: std.unittest.common.KeyFor<Bool> & std.unittest.common.KeyFor<std.core.String> & std.unittest.common.KeyFor<Int64> {
    public static prop parallel: std.unittest.KeyParallel {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyGenerationSteps <: std.unittest.common.KeyFor<Int64> {
    public static prop generationSteps: std.unittest.KeyGenerationSteps {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyReductionSteps <: std.unittest.common.KeyFor<Int64> {
    public static prop reductionSteps: std.unittest.KeyReductionSteps {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeySkip <: std.unittest.common.KeyFor<Bool> {
    public static prop skip: std.unittest.KeySkip {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuided <: std.unittest.common.KeyFor<Bool> {
    public static prop coverageGuided: std.unittest.KeyCoverageGuided {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuidedInitialSeeds <: std.unittest.common.KeyFor<Int64> {
    public static prop coverageGuidedInitialSeeds: std.unittest.KeyCoverageGuidedInitialSeeds {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuidedMaxCandidates <: std.unittest.common.KeyFor<Int64> {
    public static prop coverageGuidedMaxCandidates: std.unittest.KeyCoverageGuidedMaxCandidates {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuidedBaselineScore <: std.unittest.common.KeyFor<Int64> {
    public static prop coverageGuidedBaselineScore: std.unittest.KeyCoverageGuidedBaselineScore {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuidedNewCoverageScore <: std.unittest.common.KeyFor<Int64> {
    public static prop coverageGuidedNewCoverageScore: std.unittest.KeyCoverageGuidedNewCoverageScore {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCoverageGuidedNewCoverageBonus <: std.unittest.common.KeyFor<Int64> {
    public static prop coverageGuidedNewCoverageBonus: std.unittest.KeyCoverageGuidedNewCoverageBonus {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyBench <: std.unittest.common.KeyFor<Bool> {
    public static prop bench: std.unittest.KeyBench {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyTimeout <: std.unittest.common.KeyFor<std.core.Duration> {
    public static prop timeout: std.unittest.KeyTimeout {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyTimeoutEach <: std.unittest.common.KeyFor<std.core.String> {
    public static prop timeoutEach: std.unittest.KeyTimeoutEach {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyTimeoutHandler <: std.unittest.common.KeyFor<(std.unittest.TestCaseInfo) -> Unit> {
    public static prop timeoutHandler: std.unittest.KeyTimeoutHandler {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    public static prop captureOutput: std.unittest.KeyCaptureOutput {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyNoCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    public static prop noCaptureOutput: std.unittest.KeyNoCaptureOutput {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyShowAllOutput <: std.unittest.common.KeyFor<Bool> {
    public static prop showAllOutput: std.unittest.KeyShowAllOutput {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyVerbose <: std.unittest.common.KeyFor<Bool> {
    public static prop verbose: std.unittest.KeyVerbose {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyMeasurement <: std.unittest.common.KeyFor<std.unittest.Measurement> {
    public static prop measurement: std.unittest.KeyMeasurement {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyMeasurementInfo <: std.unittest.common.KeyFor<std.unittest.MeasurementInfo> {
    public static prop measurementInfo: std.unittest.KeyMeasurementInfo {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyInternalTestrunnerInputPath <: std.unittest.common.KeyFor<std.core.String> {
    public static prop internalTestrunnerInputPath: std.unittest.KeyInternalTestrunnerInputPath {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyDeathAware <: std.unittest.common.KeyFor<Bool> {
    public static prop deathAware: std.unittest.KeyDeathAware {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyBaseline <: std.unittest.common.KeyFor<std.core.String> {
    public static prop baseline: std.unittest.KeyBaseline {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyFilter <: std.unittest.common.KeyFor<std.core.String> {
    public static prop filter: std.unittest.KeyFilter {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyIncludeTags <: std.unittest.common.KeyFor<std.core.String> {
    public static prop includeTags: std.unittest.KeyIncludeTags {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyExcludeTags <: std.unittest.common.KeyFor<std.core.String> {
    public static prop excludeTags: std.unittest.KeyExcludeTags {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyBaselinePath <: std.unittest.common.KeyFor<std.core.String> {
    public static prop baselinePath: std.unittest.KeyBaselinePath {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyInternalWorkerId <: std.unittest.common.KeyFor<Int64> {
    public static prop internalWorkerId: std.unittest.KeyInternalWorkerId {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyInternalNWorkers <: std.unittest.common.KeyFor<Int64> {
    public static prop internalNWorkers: std.unittest.KeyInternalNWorkers {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyInternalWorkerSkipNTests <: std.unittest.common.KeyFor<Int64> {
    public static prop internalWorkerSkipNTests: std.unittest.KeyInternalWorkerSkipNTests {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyInternalMainProcessPort <: std.unittest.common.KeyFor<UInt16> {
    public static prop internalMainProcessPort: std.unittest.KeyInternalMainProcessPort {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyDryRun <: std.unittest.common.KeyFor<Bool> {
    public static prop dryRun: std.unittest.KeyDryRun {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

public struct KeyShowTags <: std.unittest.common.KeyFor<Bool> {
    public static prop showTags: std.unittest.KeyShowTags {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyNoProgress <: std.unittest.common.KeyFor<Bool> {
    public static prop noProgress: std.unittest.KeyNoProgress {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyProgressBrief <: std.unittest.common.KeyFor<Bool> {
    public static prop progressBrief: std.unittest.KeyProgressBrief {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

internal struct KeyProgressEntriesLimit <: std.unittest.common.KeyFor<Int64> {
    public static prop progressEntriesLimit: std.unittest.KeyProgressEntriesLimit {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

let OP_PRALLEL_NAME: std.core.String

let OP_PARALLEL: std.core.String

let PARALLEL_KEY: std.core.String

let N_CORES_MULTIPLICATION_PATTERN: std.regex.Regex

let DEFAULT_CORES: Int64

let MAX_N_PROCESSES: Int64

internal enum ParallelInfo {
    | NoParallel

    | Parallel(Int64)

    internal static func fromDefaultConfiguration(): std.unittest.ParallelInfo { /* compiled code */ }

    internal prop nWorkers: Int64 {
        get() { /* compiled code */ }
    }
}

internal class PowerAssertValue <: std.core.Object & std.unittest.common.Serializable<std.unittest.PowerAssertValue> {
    init(value: std.core.String, expression: std.core.String, position: Int64, isException!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    internal prop value: std.core.String {
        get() { /* compiled code */ }
    }

    internal prop range: std.core.Range<Int64> {
        get() { /* compiled code */ }
    }

    internal prop valuePosition: Int64 {
        get() { /* compiled code */ }
    }

    internal prop safeValueNextPosition: Int64 {
        get() { /* compiled code */ }
    }

    internal static func skipped(expression: std.core.String, position: Int64): std.unittest.PowerAssertValue { /* compiled code */ }

    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValue { /* compiled code */ }

    internal let kind: std.unittest.PowerAssertValueKind

    internal let expression: std.core.String

    internal let position: Int64

    PowerAssertValue(kind: std.unittest.PowerAssertValueKind, expression: std.core.String, position: Int64) { /* compiled code */ }
}

internal enum PowerAssertValueKind <: std.unittest.common.Serializable<std.unittest.PowerAssertValueKind> {
    | PAValue(std.core.String)

    | PAThrown(std.core.String)

    | PASkipped

    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValueKind { /* compiled code */ }
}

public class PowerAssertDiagramBuilder {
    private var wasExceptionThrown: Bool

    private let expression: std.core.String

    private let diagram: std.unittest.PowerAssertDiagram

    init(expression: std.core.String) { /* compiled code */ }

    public func r<T>(value: T, exprAsText: std.core.String, position: Int64): T { /* compiled code */ }

    public func r(value: Rune, exprAsText: std.core.String, position: Int64): Rune { /* compiled code */ }

    public func r(value: std.core.String, exprAsText: std.core.String, position: Int64): std.core.String { /* compiled code */ }

    public func h(exception: std.core.Exception, exprAsText: std.core.String, position: Int64): Nothing { /* compiled code */ }

    public func w(passed: Bool): Unit { /* compiled code */ }
}

internal foreign func EnableANSITerminal(): Bool

internal foreign func DisableANSITerminal(): Bool

var isConsoleUtf8: Bool

internal open class TerminalPrettyPrinter <: std.unittest.common.PrettyPrinter {
    private let areColorsEnabled: Bool

    private let areOtherAnsiEnabled: Bool

    init(configuration: std.unittest.common.Configuration) { /* compiled code */ }

    internal prop terminalHeight: std.core.Option<UInt64> {
        get() { /* compiled code */ }
    }

    internal prop terminalWidth: std.core.Option<UInt64> {
        get() { /* compiled code */ }
    }

    internal func exclusive<T>(action: (std.unittest.TerminalPrettyPrinter) -> T): T { /* compiled code */ }

    internal func up(lines: Int64): Unit { /* compiled code */ }

    internal func down(lines: Int64): Unit { /* compiled code */ }

    internal func toBottom(): Unit { /* compiled code */ }

    internal func setColumn(column!: Int64 = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func saveCursorPos(): Unit { /* compiled code */ }

    internal func restoreCursorPos(): Unit { /* compiled code */ }

    internal func hideCursor(): Unit { /* compiled code */ }

    internal func showCursor(): Unit { /* compiled code */ }

    internal func setScrollableMargins(start!: std.core.Option<Int64> = COMPILED_CODE /* compiled code */, end!: std.core.Option<Int64> = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func clearAhead(): Unit { /* compiled code */ }

    internal func clearLine(): Unit { /* compiled code */ }

    protected open override func put(s: std.core.String): Unit { /* compiled code */ }

    protected func flush(): Unit { /* compiled code */ }

    protected override func setColor(color: std.unittest.common.Color): Unit { /* compiled code */ }

    internal static prop instance: std.unittest.TerminalPrettyPrinter {
        get() { /* compiled code */ }
    }

    public func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.unittest.TerminalPrettyPrinter { /* compiled code */ }

    TerminalPrettyPrinter(isColored: Bool, isOtherAnsiAble!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal foreign func SetConsoleOutputCP(cp: UInt32): UInt32

internal func setUtf8OutputCP(): Bool { /* compiled code */ }

internal foreign func GetTerminalWidth(): UInt64

internal foreign func GetTerminalHeight(): UInt64

private class CapturedProcessResult {
    internal let stdOut: std.core.Array<UInt8>

    internal let stdErr: std.core.Array<UInt8>

    CapturedProcessResult(stdOut: std.core.Array<UInt8>, stdErr: std.core.Array<UInt8>) { /* compiled code */ }
}

internal func processStart(ctx: std.unittest.MainExecutionCtx): std.unittest.WrappedProcess { /* compiled code */ }

internal func renderException(exception: std.core.Exception, preamble: std.core.Option<std.unittest.common.PrettyText>): std.unittest.common.PrettyText { /* compiled code */ }

internal func exceptionToString(e: std.core.Exception): std.core.String { /* compiled code */ }

internal func indexOfFirstIrrelevantEntry(elements: std.core.Array<std.core.StackTraceElement>): Int64 { /* compiled code */ }

internal func isEntryPoint(_: std.core.StackTraceElement): Bool { /* compiled code */ }

internal func isLambda(element: std.core.StackTraceElement): Bool { /* compiled code */ }

internal func methodName(element: std.core.StackTraceElement): std.core.String { /* compiled code */ }

extend std.unittest.TestGroupResult {
    internal func reportCsv(dir: std.fs.Path, raw!: Bool): Unit { /* compiled code */ }
}

extend std.unittest.TestSuiteResult {
    internal func reportBenchCsv(raw!: Bool): std.core.ToString { /* compiled code */ }
}

let STYLES: std.core.String

let CASE_TEMPLATE: std.core.String

let ROOT_TEMPLATE: std.core.String

var gnuplotAvailable: Bool

extend std.unittest.TestGroupResult {
    internal func reportHtml(dir: std.fs.Path): Unit { /* compiled code */ }

    internal func collectClassObjects(passedFQNames: std.core.Iterable<std.unittest.TestCaseId>): std.collection.ArrayList<std.unittest.common.JsonObject> { /* compiled code */ }

    internal func envRow<T>(name: std.core.String, value: T): std.unittest.common.JsonObject { /* compiled code */ }

    internal func collectEnvironment(): std.unittest.common.JsonArray { /* compiled code */ }

    internal func tryGetNative(f: () -> std.core.String): std.core.String { /* compiled code */ }

    internal func collectAllResults(directory: std.fs.Path): std.collection.HashMap<std.unittest.TestCaseId, std.unittest.common.JsonValue> { /* compiled code */ }
}

internal func measurementModel(step: std.unittest.BenchStepEntry, directory: std.fs.Path, index: Int64): (Float64, std.unittest.common.JsonObject) { /* compiled code */ }

internal struct BenchStepEntry {
    internal let result: std.unittest.BenchmarkResult

    internal let arg: std.unittest.ArgumentDescription

    internal let caseId: std.unittest.TestCaseId

    internal let measurementInfo: std.unittest.MeasurementInfo

    BenchStepEntry(result: std.unittest.BenchmarkResult, arg: std.unittest.ArgumentDescription, caseId: std.unittest.TestCaseId, measurementInfo: std.unittest.MeasurementInfo) { /* compiled code */ }
}

extend std.unittest.StatisticsSample {
    internal func createStatsRow(name: std.core.String, dir: std.fs.Path, change!: std.core.Option<Float64> = COMPILED_CODE /* compiled code */): std.unittest.common.JsonObject { /* compiled code */ }

    internal func drawKde(into: std.fs.Path): Unit { /* compiled code */ }
}

extend std.unittest.BenchmarkResult {
    internal func draw(into: std.fs.Path, mName: std.core.String): Unit { /* compiled code */ }
}

let NO_INFO: std.core.String

internal func getOsVersion(): std.core.String { /* compiled code */ }

internal func getCpuName(): std.core.String { /* compiled code */ }

internal func getCompilerVersion(): std.core.String { /* compiled code */ }

internal func getTargetTriple(): std.core.String { /* compiled code */ }

internal enum TestCode <: std.core.ToString & std.unittest.common.PrettyPrintable & std.core.Equatable<std.unittest.TestCode> {
    | PASS

    | SKIP

    | TIMEOUT

    | FAIL

    | ERROR

    | NORUN

    public func toString(): std.core.String { /* compiled code */ }

    internal func mostSevereWith(other: std.unittest.TestCode): std.unittest.TestCode { /* compiled code */ }

    internal func isFailure(): Bool { /* compiled code */ }

    internal func toStringTense(): std.core.String { /* compiled code */ }

    public operator func ==(right: std.unittest.TestCode): Bool { /* compiled code */ }

    public operator func !=(right: std.unittest.TestCode): Bool { /* compiled code */ }

    public func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

internal class XmlBuilder <: std.core.Object & std.core.ToString {
    private let element_: std.unittest.XmlElementBuilder

    private var version_: std.core.ToString

    private var encoding_: std.core.Option<std.core.ToString>

    public func version(v: std.core.ToString): std.unittest.XmlBuilder { /* compiled code */ }

    public func encoding(format: std.core.ToString): std.unittest.XmlBuilder { /* compiled code */ }

    public func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlBuilder { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

internal open class XmlElementBuilder <: std.core.Object & std.core.ToString {
    private var name_: std.core.ToString

    private var attributes_: std.collection.ArrayList<(std.core.ToString, std.core.ToString)>

    private var children: std.collection.ArrayList<std.unittest.XmlElementBuilder>

    private var content_: std.core.ToString

    public mut prop attributes: std.collection.ArrayList<(std.core.ToString, std.core.ToString)> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public mut prop content: std.core.ToString {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    public func name(name: std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    public open func attribute(key: std.core.ToString, value: std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    public open func optionalAttribute(key: std.core.ToString, value: std.core.Option<std.core.String>): std.unittest.XmlElementBuilder { /* compiled code */ }

    public open func element(contentBuilder: () -> std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    public open func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlElementBuilder { /* compiled code */ }

    internal func build(sb: std.core.StringBuilder): Unit { /* compiled code */ }

    public open func toString(): std.core.String { /* compiled code */ }

    internal let level: UInt64

    XmlElementBuilder(level!: UInt64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

extend std.unittest.TestSuiteId {
    internal func asClassName(): std.core.String { /* compiled code */ }
}

extend std.unittest.TestGroupResult {
    internal func reportXml(dir: std.fs.Path): Unit { /* compiled code */ }
}

extend std.unittest.TestSuiteId {
    internal func reportFileName(isBench!: Bool, extension!: std.core.String): std.core.String { /* compiled code */ }
}

public class CsvReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    internal let directory: std.fs.Path

    CsvReporter(directory: std.fs.Path) { /* compiled code */ }
}

public class CsvRawReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    internal let directory: std.fs.Path

    CsvRawReporter(directory: std.fs.Path) { /* compiled code */ }
}

public class XmlReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> {
    internal let directory: std.fs.Path

    XmlReporter(directory: std.fs.Path) { /* compiled code */ }
}

public class ConsoleReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    internal let colored: Bool

    ConsoleReporter(colored!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

public class TextReporter<PP> <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    internal let into: PP

    TextReporter(into!: PP) { /* compiled code */ }
}

internal interface TextReporterBase<PP> <: std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    public open func printReport(report: std.unittest.Report): PP
}

extend std.unittest.TextReporter<PP> <: TextReporterBase {
    public func printReport(report: std.unittest.Report): PP { /* compiled code */ }
}

extend std.unittest.ConsoleReporter <: TextReporterBase {
    public func printReport(report: std.unittest.Report): Unit { /* compiled code */ }
}

public class RawStatsReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, std.collection.HashMap<std.core.String, (Float64, Float64)>> {
    internal func benchResultsAsMap(result: std.unittest.TestGroupResult): std.collection.HashMap<std.core.String, (Float64, Float64)> { /* compiled code */ }

    RawStatsReporter() { /* compiled code */ }
}

internal class ProgressReporter {
    private let reporter: std.unittest.UTProgressReporter

    private var terminalHeight: std.core.Option<UInt64>

    private var terminalWidth: std.core.Option<UInt64>

    private var prevReportHeight: Int64

    private let printer: std.unittest.TerminalPrettyPrinter

    private var reporterJob: std.core.Option<std.core.Future<Unit>>

    private let isActive: std.sync.AtomicBool

    internal prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress> {
        get() { /* compiled code */ }
    }

    internal func startReporting(): Unit { /* compiled code */ }

    internal func stopAndClear(): Unit { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.core.Option<std.unittest.ProgressReporter> { /* compiled code */ }
}

internal interface UTProgressReporter {
    public open prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    public open func start(): Unit { /* compiled code */ }

    public open func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText

    public open func reset(): Unit { /* compiled code */ }
}

internal class TestProgressReporter <: std.core.Object & std.unittest.UTProgressReporter {
    private var width: Int64

    private var height: Int64

    private var markWidth: Int64

    private let booker: std.unittest.TestProgressCollector

    public override prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress> {
        get() { /* compiled code */ }
    }

    public override func start(): Unit { /* compiled code */ }

    public override func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText { /* compiled code */ }

    internal let testCasesParallelism: Int64

    internal let isBrief: Bool

    internal let entriesLimit: std.core.Option<UInt64>

    TestProgressReporter(testCasesParallelism: Int64, isBrief!: Bool, entriesLimit!: std.core.Option<UInt64> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

private class TestProgressCollector {
    internal let updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    private let registeredWorkers: std.collection.HashSet<std.unittest.UniqueWorkerId>

    private let totalPackageCasesCount: std.collection.HashMap<std.core.String, UInt64>

    private let packagesOrder: std.collection.ArrayList<std.core.String>

    private let packagesStartTime: std.collection.HashMap<std.core.String, std.time.DateTime>

    private let finishedPackageCasesCount: std.collection.HashMap<std.core.String, UInt64>

    private let packagesProgress: std.collection.HashMap<std.core.String, std.unittest.Progress>

    private let failedPackages: std.collection.HashSet<std.core.String>

    private var _totalCasesCount: UInt64

    private let casesOrder: std.core.Array<std.core.Option<Int64>>

    private var _finishedCasesCount: UInt64

    private var _passedCasesCount: UInt64

    private var _failedCasesCount: UInt64

    private var _crashedWorkers: UInt64

    private var _lastFailedStep: std.core.Option<std.unittest.FailedTestCaseProgressData>

    private let summaryProgress: std.unittest.Progress

    private var startTime: std.time.MonoTime

    private let executing: std.collection.HashMap<Int64, std.unittest.TestCaseProgressData>

    private let finishedPackages: std.collection.ArrayList<std.core.String>

    internal prop totalCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop finishedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop passedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop failedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop crashedWorkersCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop registeredPackagesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop currentPackagesCount: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop lastFailedStep: std.core.Option<std.unittest.FailedTestCaseProgressData> {
        get() { /* compiled code */ }
    }

    internal prop currentPackages: std.core.Array<std.unittest.PackageProgressData> {
        get() { /* compiled code */ }
    }

    internal prop currentTestCases: std.core.Array<std.unittest.TestCaseProgressData> {
        get() { /* compiled code */ }
    }

    internal prop summary: std.unittest.SummaryProgressData {
        get() { /* compiled code */ }
    }

    internal func start(): Unit { /* compiled code */ }

    internal func flushChanges(): Unit { /* compiled code */ }

    TestProgressCollector(testCasesParallelism: Int64) { /* compiled code */ }
}

private struct UniqueWorkerId <: std.core.Hashable & std.core.Equatable<std.unittest.UniqueWorkerId> {
    public func hashCode(): Int64 { /* compiled code */ }

    public operator func ==(other: std.unittest.UniqueWorkerId): Bool { /* compiled code */ }

    public operator func !=(other: std.unittest.UniqueWorkerId): Bool { /* compiled code */ }

    internal let id: Int64

    internal let pack: std.core.String

    UniqueWorkerId(id!: Int64, pack!: std.core.String) { /* compiled code */ }
}

private class Progress <: std.core.Object & std.unittest.common.PrettyPrintable {
    private var text: std.unittest.common.PrettyText

    private let pieces: std.collection.ArrayList<Bool>

    private var barIdx: Int64

    private var drawnPieces: Int64

    private var prevWidth: Int64

    private var prevPieceWidth: Int64

    private var prevPieceStatus: Bool

    private let POSITIVE_COLOR: std.unittest.common.Color

    private let NEGATIVE_COLOR: std.unittest.common.Color

    private let GRAY_COLOR: std.unittest.common.Color

    internal func update(positive!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func expand(extendBy: UInt64): Unit { /* compiled code */ }

    public func pprint(to: std.unittest.common.PrettyPrinter, crashedWorkers!: UInt64 = COMPILED_CODE /* compiled code */, width!: Int64, mainBar!: Bool = COMPILED_CODE /* compiled code */): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public override func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    internal var totalPieces: Int64

    Progress(totalPieces!: Int64) { /* compiled code */ }
}

private struct FailedTestCaseProgressData {
    internal func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    internal let name: std.unittest.TestCaseName

    internal let suiteId: std.unittest.TestSuiteId

    internal let check: std.unittest.RunStepResult

    FailedTestCaseProgressData(name: std.unittest.TestCaseName, suiteId!: std.unittest.TestSuiteId, check!: std.unittest.RunStepResult) { /* compiled code */ }
}

private struct TestCaseProgressData {
    internal prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    internal func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    internal static func trim(suiteId: std.unittest.TestSuiteId, name: std.unittest.TestCaseName, limit!: Int64): std.core.String { /* compiled code */ }

    internal let name: std.unittest.TestCaseName

    internal let suiteId: std.unittest.TestSuiteId

    internal let startTime: std.time.DateTime

    TestCaseProgressData(name: std.unittest.TestCaseName, suiteId!: std.unittest.TestSuiteId, startTime!: std.time.DateTime) { /* compiled code */ }
}

private struct PackageProgressData {
    internal prop finishPercent: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    internal func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    internal let name: std.core.String

    internal let totalCases: UInt64

    internal let finishedCases: UInt64

    internal let isFailed: Bool

    internal let startTime: std.time.DateTime

    internal let progress: std.unittest.Progress

    PackageProgressData(name: std.core.String, totalCases!: UInt64, finishedCases!: UInt64, isFailed!: Bool, startTime!: std.time.DateTime, progress!: std.unittest.Progress) { /* compiled code */ }
}

private struct SummaryProgressData {
    internal prop finishPercent: UInt64 {
        get() { /* compiled code */ }
    }

    internal prop isFailed: Bool {
        get() { /* compiled code */ }
    }

    internal prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    internal let passedCases: UInt64

    internal let failedCases: UInt64

    internal let finishedCases: UInt64

    internal let totalCases: UInt64

    internal let crashedWorkers: UInt64

    internal let startTime: std.time.MonoTime

    internal let progress: std.unittest.Progress

    SummaryProgressData(passedCases!: UInt64, failedCases!: UInt64, finishedCases!: UInt64, totalCases!: UInt64, crashedWorkers!: UInt64, startTime!: std.time.MonoTime, progress!: std.unittest.Progress) { /* compiled code */ }
}

internal struct TestProgressData <: std.unittest.UTProgress {
    internal let data: std.unittest.ExecutionResultPart

    internal let workerId: Int64

    TestProgressData(data: std.unittest.ExecutionResultPart, workerId!: Int64 = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

private enum TestCaseName <: std.core.ToString {
    | CaseName(std.unittest.TestCaseId)

    | CNStep(std.unittest.LStep)

    public override func toString(): std.core.String { /* compiled code */ }
}

extend std.unittest.ExecutionResultPart {
    internal func toCaseName(): std.unittest.TestCaseName { /* compiled code */ }
}

extend std.unittest.ExecutionResultBody {
    internal prop isSkip: Bool {
        get() { /* compiled code */ }
    }

    internal prop isFailed: Bool {
        get() { /* compiled code */ }
    }

    internal prop firstFailedStep: std.unittest.RunStepResult {
        get() { /* compiled code */ }
    }

    internal prop isPassed: Bool {
        get() { /* compiled code */ }
    }

    internal prop isClassLifecycle: Bool {
        get() { /* compiled code */ }
    }

    internal prop isBeforeAll: Bool {
        get() { /* compiled code */ }
    }
}

extend std.unittest.TestCaseId {
    internal func toDisplayString(): std.core.String { /* compiled code */ }
}

extend std.unittest.TestSuiteId {
    internal func toDisplayString(): std.core.String { /* compiled code */ }
}

internal struct ArgumentDescription {
    internal func intoErrorMessage(): std.core.Option<std.unittest.common.PrettyText> { /* compiled code */ }

    internal let textDescription: std.core.Option<std.unittest.common.PrettyText>

    internal let stepIndex: Int64

    internal let reductionIndex: Int64

    internal let randomSeed: std.core.Option<Int64>

    ArgumentDescription(textDescription: std.core.Option<std.unittest.common.PrettyText>, stepIndex: Int64, reductionIndex: Int64, randomSeed: std.core.Option<Int64>) { /* compiled code */ }
}

internal struct RenderOptions {
    internal static func fromConfiguration(configuration: std.unittest.common.Configuration): std.unittest.RenderOptions { /* compiled code */ }

    internal let baselineString: std.core.Option<std.core.String>

    internal let measurementInfo: std.core.Option<std.unittest.MeasurementInfo>

    RenderOptions(baselineString: std.core.Option<std.core.String>, measurementInfo: std.core.Option<std.unittest.MeasurementInfo>) { /* compiled code */ }
}

internal enum GroupResultPart <: std.unittest.Result {
    | SuiteResult(std.unittest.TestSuiteResult)

    | ProcessError(std.unittest.TestProcessError)

    public func walk(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    public func visitAll<V>(visitor: (V) -> Unit): Unit { /* compiled code */ }

    public prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }
}

extend std.unittest.LStep <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.LStep { /* compiled code */ }
}

extend std.unittest.StepKind <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepKind { /* compiled code */ }
}

extend std.unittest.StepInfo <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepInfo { /* compiled code */ }
}

extend std.unittest.RunStepResult {
    internal func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    internal static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.RunStepResult { /* compiled code */ }
}

extend std.unittest.TestCaseResult {
    internal func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    internal static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseResult { /* compiled code */ }
}

extend std.unittest.TestCaseReportInfo <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseReportInfo { /* compiled code */ }
}

extend std.unittest.TestSuiteResult {
    internal func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    internal static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteResult { /* compiled code */ }
}

extend std.unittest.TestSuiteReportInfo <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteReportInfo { /* compiled code */ }
}

extend std.unittest.TestGroupResult {
    internal func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    internal static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestGroupResult { /* compiled code */ }
}

extend std.unittest.RenderOptions <: Serializable {
    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.RenderOptions { /* compiled code */ }
}

internal struct ArrayOfTuples<A, B> <: std.unittest.common.Serializable<std.unittest.ArrayOfTuples<A, B>> {
    init(col: std.core.Collection<(A, B)>) { /* compiled code */ }

    internal func original(): std.core.Array<(A, B)> { /* compiled code */ }

    public func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    public static func deserialize(dm: std.unittest.common.DataModel): std.unittest.ArrayOfTuples<A, B> { /* compiled code */ }

    internal let left: std.core.Array<A>

    internal let right: std.core.Array<B>

    ArrayOfTuples(left: std.core.Array<A>, right: std.core.Array<B>) { /* compiled code */ }
}

let MEDIAN_CI_95_COEFF: Float64

let MEDIAN_CI_99_COEFF: Float64

let MEDIAN_CI_999_COEFF: Float64

let EPSILON: Float64

internal class Measurements {
    internal var slopes: std.unittest.Sample

    internal var mainSlope: Float64

    internal var intercept: Float64

    internal var errorEst: Float64

    internal var r2: Float64

    internal func calculateMedian(): std.unittest.Measurements { /* compiled code */ }

    internal func calcSlopes(): Unit { /* compiled code */ }

    internal func calculateMean(): std.unittest.Measurements { /* compiled code */ }

    private let data: std.core.Array<(Float64, Float64)>

    Measurements(data: std.core.Array<(Float64, Float64)>) { /* compiled code */ }
}

internal interface AsFloat {
    public open func asFloat(): Float64
}

extend Float64 <: AsFloat {
    public func asFloat(): Float64 { /* compiled code */ }
}

public class FlatMapProcessor<T, R> <: std.unittest.DataStrategyProcessor<R> {
    internal var provider: std.unittest.RememberingDataProvider<R> = COMPILED_CODE /* compiled code */

    protected prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    protected func lastItem(_: std.unittest.common.Configuration): R { /* compiled code */ }

    internal let orig: std.unittest.DataStrategyProcessor<T>

    internal let mapper: (T) -> std.unittest.common.DataProvider<R>

    FlatMapProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataProvider<R>) { /* compiled code */ }
}

public open class LazyCyclicNode {
    internal var next_: std.unittest.LazyCyclicNode

    internal var prev: std.unittest.LazyCyclicNode

    internal func initialize(): std.unittest.LazyCyclicNode { /* compiled code */ }

    protected open func advance(): std.core.Option<Unit> { /* compiled code */ }

    protected open func recover(): Unit { /* compiled code */ }

    internal func add(other: std.unittest.LazyCyclicNode): Unit { /* compiled code */ }

    internal func removeSelf(): std.unittest.LazyCyclicNode { /* compiled code */ }

    init() { /* compiled code */ }
}

public class FlatMapStrategyProcessor<T, R> <: std.unittest.DataStrategyProcessor<R> {
    internal var provider: std.unittest.RememberingDataProvider<R> = COMPILED_CODE /* compiled code */

    internal var strategy: std.unittest.common.DataStrategy<R> = COMPILED_CODE /* compiled code */

    protected prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    protected func lastItem(_: std.unittest.common.Configuration): R { /* compiled code */ }

    internal let orig: std.unittest.DataStrategyProcessor<T>

    internal let mapper: (T) -> std.unittest.common.DataStrategy<R>

    FlatMapStrategyProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataStrategy<R>) { /* compiled code */ }
}

public class MapProcessor<T, R> <: std.unittest.DataStrategyProcessor<R> {
    protected prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    protected func lastItem(configuration: std.unittest.common.Configuration): R { /* compiled code */ }

    internal let orig: std.unittest.DataStrategyProcessor<T>

    internal let mapper: (T, std.unittest.common.Configuration) -> R

    MapProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T, std.unittest.common.Configuration) -> R) { /* compiled code */ }
}

public class CartesianProductProcessor<T0, T1> <: std.unittest.DataStrategyProcessor<(T0, T1)> {
    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    protected prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    protected func lastItem(configuration: std.unittest.common.Configuration): (T0, T1) { /* compiled code */ }

    internal let left: std.unittest.DataStrategyProcessor<T0>

    internal let right: std.unittest.DataStrategyProcessor<T1>

    CartesianProductProcessor(left: std.unittest.DataStrategyProcessor<T0>, right: std.unittest.DataStrategyProcessor<T1>) { /* compiled code */ }
}

internal class CartesianProductIterator<A, B> <: std.core.Iterator<(A, B)> {
    internal var leftElement: std.core.Option<A>

    internal var right: std.core.Iterator<B>

    public override func next(): std.core.Option<(A, B)> { /* compiled code */ }

    internal let left: std.core.Iterator<A>

    internal let rightGen: () -> std.core.Iterator<B>

    CartesianProductIterator(left: std.core.Iterator<A>, rightGen: () -> std.core.Iterator<B>) { /* compiled code */ }
}

public class SimpleProcessor<T> <: std.unittest.DataStrategyProcessor<T> {
    internal var dataProvider: std.unittest.RememberingDataProvider<T> = COMPILED_CODE /* compiled code */

    private var _delegate: std.core.Option<std.unittest.common.DataStrategy<T>> = COMPILED_CODE /* compiled code */

    private prop strategy: std.unittest.common.DataStrategy<T> {
        get() { /* compiled code */ }
    }

    protected prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    protected func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T> { /* compiled code */ }

    protected func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T> { /* compiled code */ }

    protected func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    protected func lastItem(_: std.unittest.common.Configuration): T { /* compiled code */ }

    internal let buildDelegate: () -> std.unittest.common.DataStrategy<T>

    internal let name: std.core.String

    SimpleProcessor(buildDelegate: () -> std.unittest.common.DataStrategy<T>, name: std.core.String) { /* compiled code */ }
}

internal class RememberingIterable<T> <: std.core.Iterator<T> {
    public func next(): std.core.Option<T> { /* compiled code */ }

    internal let delegate: std.core.Iterator<T>

    internal let source: std.unittest.RememberingDataProvider<T>

    RememberingIterable(delegate: std.core.Iterator<T>, source: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

internal class RememberingLazyIterator<T> <: std.core.Iterator<T> {
    public func next(): std.core.Option<T> { /* compiled code */ }

    internal let delegate: std.unittest.RememberingLazyIterable<T>

    RememberingLazyIterator(delegate: std.unittest.RememberingLazyIterable<T>) { /* compiled code */ }
}

internal class RememberingLazyIterable<T> <: std.unittest.LazyCyclicNode & std.core.Iterable<T> {
    internal var currentItem: std.core.Option<T> = COMPILED_CODE /* compiled code */

    internal var origLastItem: T

    public func recover(): Unit { /* compiled code */ }

    public func advance(): std.core.Option<Unit> { /* compiled code */ }

    public func iterator(): std.core.Iterator<T> { /* compiled code */ }

    internal let delegate: std.core.Iterator<T>

    internal let storage: std.unittest.RememberingDataProvider<T>

    RememberingLazyIterable(delegate: std.core.Iterator<T>, storage: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

internal class IterableMix<T> <: std.core.Iterator<T> {
    public func next(): std.core.Option<T> { /* compiled code */ }

    internal let delegate1: std.core.Iterator<T>

    internal let delegate2: std.core.Iterator<T>

    IterableMix(delegate1: std.core.Iterator<T>, delegate2: std.core.Iterator<T>) { /* compiled code */ }
}

internal class LazyCyclicNodeEngine<T> <: std.core.Iterator<T> {
    public func next(): std.core.Option<T> { /* compiled code */ }

    public func advance(): std.core.Option<Unit> { /* compiled code */ }

    internal var current: std.unittest.LazyCyclicNode

    internal let iter: std.core.Iterator<T>

    LazyCyclicNodeEngine(current: std.unittest.LazyCyclicNode, iter: std.core.Iterator<T>) { /* compiled code */ }
}

internal type ShrinkResult = std.unittest.RunStepResult

internal enum ProcessorResult {
    | Success(std.unittest.RunStepResult)

    | Failure(std.core.Array<std.unittest.InputParameter>, std.unittest.RunStepResult)

    | NoItems

    | StrategyError(std.core.Exception)
}

extend std.unittest.DataStrategyProcessor<T> {
    public func map<R>(f: (T) -> R): std.unittest.MapProcessor<T, R> { /* compiled code */ }

    public func mapWithConfig<R>(f: (T, std.unittest.common.Configuration) -> R): std.unittest.MapProcessor<T, R> { /* compiled code */ }

    public func flatMap<R>(f: (T) -> std.unittest.common.DataProvider<R>): std.unittest.FlatMapProcessor<T, R> { /* compiled code */ }

    public func flatMapStrategy<R>(f: (T) -> std.unittest.common.DataStrategy<R>): std.unittest.FlatMapStrategyProcessor<T, R> { /* compiled code */ }

    public func product<R>(p: std.unittest.DataStrategyProcessor<R>): std.unittest.CartesianProductProcessor<T, R> { /* compiled code */ }

    public func productWithUnit<P>(p: P): std.unittest.MapProcessor<(T, Unit), T> { /* compiled code */ }
}

internal class TestSuiteExecutor {
    private let suiteConfiguration: std.unittest.common.Configuration

    private let suiteInfo: std.unittest.TestSuiteReportInfo

    internal func execute(): std.unittest.TestSuiteResult { /* compiled code */ }

    internal func runSkip(testCaseResult: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    internal func runNoRun(testCaseResult: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    internal func runSingleCase(caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor): Unit { /* compiled code */ }

    private let externalConfiguration: std.unittest.common.Configuration

    private let suiteId: std.unittest.TestSuiteId

    private let testSuite: std.unittest.TestSuite

    private let filterService: std.unittest.FilterService

    TestSuiteExecutor(externalConfiguration: std.unittest.common.Configuration, suiteId: std.unittest.TestSuiteId, testSuite: std.unittest.TestSuite, filterService: std.unittest.FilterService) { /* compiled code */ }
}

extend std.unittest.TestSuite {
    internal prop wholeConfiguration: std.unittest.common.Configuration {
        get() { /* compiled code */ }
    }

    internal prop allCasesIterator: std.core.Iterable<std.unittest.CaseOrBench> {
        get() { /* compiled code */ }
    }

    internal func allCasesToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): std.core.Array<std.unittest.CaseOrBench> { /* compiled code */ }

    internal func casesCountToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): Int64 { /* compiled code */ }

    internal func runBeforeEach(caseShortName: std.core.String): Unit { /* compiled code */ }

    internal func runAfterEach(caseShortName: std.core.String): Unit { /* compiled code */ }

    internal func runBeforeAll(): Unit { /* compiled code */ }

    internal func runAfterAll(): Unit { /* compiled code */ }
}

internal class TempDirectory {
    private let lock: std.sync.ReentrantMutex

    private let rnd: std.random.Random

    private let buffer: std.core.Array<Rune>

    private var currentOffset: Int64

    private var tempDir: std.core.Option<std.fs.Path>

    internal prop path: std.fs.Path {
        get() { /* compiled code */ }
    }

    internal func createTempFile(dir!: std.fs.Path = COMPILED_CODE /* compiled code */, prefix!: std.core.String = COMPILED_CODE /* compiled code */, suffix!: std.core.String = COMPILED_CODE /* compiled code */): std.fs.File { /* compiled code */ }

    internal func createTempPath(dir!: std.fs.Path = COMPILED_CODE /* compiled code */, prefix!: std.core.String = COMPILED_CODE /* compiled code */, suffix!: std.core.String = COMPILED_CODE /* compiled code */): std.fs.Path { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func substituteTemplate(template: std.core.String, values: std.unittest.common.JsonValue): std.core.String { /* compiled code */ }

internal class Root {
    internal var templates: std.collection.HashMap<std.core.String, std.unittest.Template>

    internal var rootTemplate: std.unittest.Template

    internal func substitute(values: std.unittest.common.JsonValue): std.core.String { /* compiled code */ }

    Root(t: std.core.String) { /* compiled code */ }
}

internal class Template {
    internal static func fromString(text: std.core.String): std.unittest.Template { /* compiled code */ }

    internal static var REPEATABLES_MATCHER: std.regex.Regex

    internal static var VARIABLES_MATCHER: std.regex.Regex

    internal static let REPETITIONS_SANITY_CHECK: Int64

    internal func substitute(values: std.unittest.common.JsonValue, templates: std.collection.HashMap<std.core.String, std.unittest.Template>, index: Int64): std.core.Option<std.core.String> { /* compiled code */ }

    internal let name: std.core.String

    internal let baseText: std.core.String

    Template(name: std.core.String, baseText: std.core.String) { /* compiled code */ }
}

internal func object(data: std.core.Array<(std.core.String, std.unittest.IntoJson)>): std.unittest.common.JsonObject { /* compiled code */ }

internal interface IntoJson {
    public open func json(): std.unittest.common.JsonValue
}

extend std.unittest.common.JsonValue <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend std.core.String <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend Int64 <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend std.core.Option<T> <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend std.core.Array<T> <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: IntoJson {
    public func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

internal func runTestStep(caseId: std.unittest.TestCaseId, f: () -> Unit): Unit { /* compiled code */ }

internal class TestCaseExecutor<T> <: std.core.Object & std.unittest.Executor {
    internal var generationStep: Int64 = COMPILED_CODE /* compiled code */

    internal var randomSeed: std.core.Option<Int64> = COMPILED_CODE /* compiled code */

    internal var hasMoreParameters: Bool = COMPILED_CODE /* compiled code */

    public func execute(_: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    internal static func formatArguments(inputs: std.core.Array<std.unittest.InputParameter>): std.core.Option<std.unittest.common.PrettyText> { /* compiled code */ }

    private let strategy: std.unittest.DataStrategyProcessor<T>

    private let body: (T) -> Unit

    TestCaseExecutor(strategy: std.unittest.DataStrategyProcessor<T>, body: (T) -> Unit) { /* compiled code */ }
}

extend std.unittest.RunStepResult {
    internal func withUpdatedStep(newKind: std.unittest.StepKind): std.unittest.RunStepResult { /* compiled code */ }
}

internal func strategyErrorAsResult(e: std.core.Exception, descr: std.unittest.ArgumentDescription): std.unittest.RunStepResult { /* compiled code */ }

let SHOW_ALL_OUTPUT_KEY: std.core.String

let NO_CAPTURE_OUTPUT_KEY: std.core.String

let OP_CAPTURE_OUTPUT: std.core.String

let CAPTURE_OUTPUT_KEY: std.core.String

let OP_VERBOSE: std.core.String

let VERBOSE_KEY: std.core.String

internal func tagsToString(tags: std.core.Array<std.core.String>): std.core.Option<std.core.String> { /* compiled code */ }

extend std.unittest.common.Configuration {
    internal prop bench: Bool {
        get() { /* compiled code */ }
    }
}

internal class PackageTextReport {
    internal var baselineReport: std.collection.HashMap<std.core.String, Float64>

    internal var baselineId: std.core.String

    internal var failures: std.collection.HashMap<std.unittest.TestCaseId, Int64>

    internal func printPackage(packageResult: std.unittest.TestGroupResult): Unit { /* compiled code */ }

    internal let pp: std.unittest.common.PrettyPrinter

    internal let outputReporter: std.unittest.TestOutputReporter

    internal let config: std.unittest.common.Configuration

    internal let detailed: Bool

    PackageTextReport(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration, detailed!: Bool) { /* compiled code */ }
}

internal func appendDashLine(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

internal class TextReportEntry {
    internal let checkResults: std.core.Array<std.unittest.CheckResult>

    internal let duration: std.core.Duration

    internal let statusCode: std.unittest.TestCode

    internal let preamble: std.unittest.common.PrettyText

    internal let caseInfo: std.unittest.TestCaseReportInfo

    internal let bodyMessage: std.core.Option<std.unittest.common.PrettyText>

    TextReportEntry(source: std.unittest.RunStepResult, caseName: std.core.String, caseInfo: std.unittest.TestCaseReportInfo, bodyMessage!: std.core.Option<std.unittest.common.PrettyText> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal class ProjectTextReport {
    internal func printProject(projectResult: std.unittest.ProjectExecutionResult): Unit { /* compiled code */ }

    private let pp: std.unittest.common.PrettyPrinter

    private let outputReporter: std.unittest.TestOutputReporter

    private let config: std.unittest.common.Configuration

    ProjectTextReport(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration) { /* compiled code */ }
}

internal class TextReports {
    internal static func printDefaultReport(pp: std.unittest.common.PrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    internal static func printProjectSummaryReport(projectResult: std.unittest.ProjectExecutionResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    internal static func printIntermediatePackageResult(pp: std.unittest.TerminalPrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal func lifecycleStageFailedText(ls: std.unittest.LStep, newLine!: Bool = COMPILED_CODE /* compiled code */): std.unittest.common.PrettyText { /* compiled code */ }

internal func printSummary<T>(pp: std.unittest.common.PrettyPrinter, result: std.unittest.ResultContainer<T>, config: std.unittest.common.Configuration, printFailures!: Bool): Unit { /* compiled code */ }

extend std.unittest.CheckResult <: PrettyPrintable {
    public func pprintWithFailedPrefix(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    public func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

let OP_TIMEOUT_EACH_NAME: std.core.String

let OP_TIMEOUT_EACH: std.core.String

let TIMEOUT_EACH_KEY: std.core.String

let VALUE_TIMEOUT_PATTERN: std.regex.Regex

internal func cancellationPoint(): Unit { /* compiled code */ }

internal func fillTimeoutKey(configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

internal class TransportMain <: std.core.Object & std.core.Resource {
    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }

    internal prop port: UInt16 {
        get() { /* compiled code */ }
    }

    internal func accept(): std.unittest.TransportChannel { /* compiled code */ }

    internal static func fromDefaultConfiguration(): std.unittest.TransportMain { /* compiled code */ }

    private let socket: std.net.TcpServerSocket

    private let outputReporter: std.unittest.TestOutputReporter
}

internal struct WorkerOutput {
    internal prop stdouts: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    internal prop stderrs: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    internal prop captured: std.core.Array<std.unittest.CapturedOutput> {
        get() { /* compiled code */ }
    }

    internal let stdout: std.core.Array<UInt8>

    internal let stderr: std.core.Array<UInt8>

    WorkerOutput(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }
}

public open class UnittestException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

internal class ErrorWrapperException <: std.core.Exception {
    init(error: std.core.Error) { /* compiled code */ }

    init(message: std.core.String, error: std.core.Error) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

public class UnittestTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

internal foreign func CJ_FS_IsFileORLinkToFile(path: CString): Int8

internal foreign func CJ_FS_IsDirORLinkToFDir(path: CString): Int8

extend std.unittest.common.DataModel {
    internal func asStruct(): std.unittest.common.DataModelStruct { /* compiled code */ }
}

internal func min<T>(a: T, b: T): T { /* compiled code */ }

internal func max<T>(a: T, b: T): T { /* compiled code */ }

internal func mapParallelOrdered<T, V>(bufferSize: Int64, f: (T) -> V): std.core.Iterator<V> { /* compiled code */ }

internal class ParallelOrderedMapIterator<T> <: std.core.Iterator<T> {
    internal var futures: std.collection.ArrayList<std.core.Future<T>>

    internal var currentIdx: Int64 = COMPILED_CODE /* compiled code */

    public func next(): std.core.Option<T> { /* compiled code */ }

    internal let bufferSize: Int64

    internal let iter: std.core.Iterator<std.core.Future<T>>

    ParallelOrderedMapIterator(bufferSize: Int64, iter: std.core.Iterator<std.core.Future<T>>) { /* compiled code */ }
}

internal class ChainIterator<T> <: std.core.Iterator<T> {
    public override func next(): std.core.Option<T> { /* compiled code */ }

    internal let l: std.core.Iterator<T>

    internal let r: std.core.Iterator<T>

    ChainIterator(l: std.core.Iterator<T>, r: std.core.Iterator<T>) { /* compiled code */ }
}

internal func chain<T>(l: std.core.Iterable<T>, r: std.core.Iterable<T>): std.core.Iterable<T> { /* compiled code */ }

internal func beforeEach<T>(action: (std.core.Option<T>) -> Unit): std.core.Iterator<T> { /* compiled code */ }
