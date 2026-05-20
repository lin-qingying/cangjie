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

let MEASUREMENTS_LIMIT: Int64

let MAX_BATCH_SIZE: Int64

let MIN_COLUMN_WIDTH: Int64

struct ImmutableInputProvider <: std.unittest.BenchInputProvider<T> {
    mut func `get`(_: Int64): T { /* compiled code */ }

    static func createOrExisting(arg: T, x: Int64): std.unittest.ImmutableInputProvider<T> { /* compiled code */ }

    static func createOrExisting(arg: U): U { /* compiled code */ }

    let data: T

    ImmutableInputProvider(data: T) { /* compiled code */ }
}

interface BenchInputProvider <: std.unittest.BenchmarkInputMarker {
    mut func reset(max: Int64): Unit { /* compiled code */ }

    abstract mut func `get`(idx: Int64): T
}

struct BatchInputProvider <: std.unittest.BenchInputProvider<T> {
    var data: std.core.Array<T>

    mut func reset(max: Int64): Unit { /* compiled code */ }

    mut func `get`(idx: Int64): T { /* compiled code */ }

    let builder: () -> T

    BatchInputProvider(builder: () -> T) { /* compiled code */ }
}

struct BatchSizeOneInputProvider <: std.unittest.BenchInputProvider<T> {
    var data: T

    mut func reset(max: Int64): Unit { /* compiled code */ }

    mut func `get`(_: Int64): T { /* compiled code */ }

    let builder: () -> T

    BatchSizeOneInputProvider(builder: () -> T) { /* compiled code */ }
}

struct GenerateEachInputProvider <: std.unittest.BenchInputProvider<T> {
    mut func reset(_: Int64): Unit { /* compiled code */ }

    mut func `get`(_: Int64): T { /* compiled code */ }

    let builder: () -> T

    GenerateEachInputProvider(builder: () -> T) { /* compiled code */ }
}

class TestSuiteResult <: std.unittest.ResultContainer<std.unittest.TestCaseResult> {
    func merge(other: std.unittest.TestSuiteResult): Unit { /* compiled code */ }

    static func mergeAll(suiteResults: std.core.Iterable<std.unittest.TestSuiteResult>): std.core.Collection<std.unittest.TestSuiteResult> { /* compiled code */ }

    let suiteId: std.unittest.TestSuiteId

    let suiteInfo: std.unittest.TestSuiteReportInfo

    TestSuiteResult(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo) { /* compiled code */ }
}

interface BenchTable {
    static func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable> { /* compiled code */ }

    abstract func doBuild(): std.unittest.PrettyTable

    abstract prop hasBaselineHeader: Bool
}

class BenchTableImpl <: std.core.Object & std.unittest.BenchTable {
    let subgroups: std.collection.ArrayList<std.unittest.BenchTableImpl>

    prop hasBaselineHeader: Bool {
        get() { /* compiled code */ }
    }

    func putResultToBaselineReport(key: std.core.String, value: Float64): Unit { /* compiled code */ }

    func getResultFromBaselineReport(key: std.core.String): std.core.Option<Float64> { /* compiled code */ }

    static func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable> { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    func initTableStructure(): Unit { /* compiled code */ }

    func doBuild(): std.unittest.PrettyTable { /* compiled code */ }

    func doTableBuild(): Unit { /* compiled code */ }

    func addSubgroup(subgroup: std.unittest.BenchTableImpl): Unit { /* compiled code */ }

    let props: std.unittest.BenchTableProperties

    let table: std.unittest.PrettyTable

    BenchTableImpl(props: std.unittest.BenchTableProperties, table: std.unittest.PrettyTable) { /* compiled code */ }
}

foreign func GetRdtscp(): UInt64

interface TestFilter {
    abstract func shouldSkipTestClass(suiteFilterKey: std.unittest.SuiteFilterKey): Bool

    abstract func shouldSkipTestCase(caseFilterKey: std.unittest.CaseFilterKey): Bool
}

class CompositeTestFilter <: std.core.Object & std.unittest.TestFilter {
    override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    let filters: std.core.Array<std.unittest.TestFilter>

    CompositeTestFilter(filters: std.core.Array<std.unittest.TestFilter>) { /* compiled code */ }
}

class TagsTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    let includeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    let excludeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    init(includeArgs: std.core.Array<std.core.String>, excludeArgs: std.core.Array<std.core.String>) { /* compiled code */ }

    redef static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TagsTestFilter { /* compiled code */ }

    redef static func fromDefaultConfiguration(): std.unittest.TagsTestFilter { /* compiled code */ }

    override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }
}

class FilterOptionTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    let includeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    let excludeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    init() { /* compiled code */ }

    init(filterArgs: std.core.Array<std.core.String>) { /* compiled code */ }

    redef static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.FilterOptionTestFilter { /* compiled code */ }

    redef static func fromDefaultConfiguration(): std.unittest.FilterOptionTestFilter { /* compiled code */ }

    override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }
}

interface UserTestFilter <: std.unittest.TestFilter {
    static func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TestFilter { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.TestFilter { /* compiled code */ }
}

func defaultConfiguration(): std.unittest.common.Configuration { /* compiled code */ }

struct SuiteFilterKey {
    static func fromTestSuite(groupName: std.core.String, suite: std.unittest.TestSuite): std.unittest.SuiteFilterKey { /* compiled code */ }

    let suiteId: std.unittest.TestSuiteId

    let tags: std.core.Array<std.core.String>

    let topLevelFunc: std.core.Option<std.core.String>

    SuiteFilterKey(suiteId: std.unittest.TestSuiteId, tags: std.core.Array<std.core.String>, topLevelFunc: std.core.Option<std.core.String>) { /* compiled code */ }
}

class TestSuite {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    func runTests(): std.unittest.TestReport { /* compiled code */ }

    func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }

    func runBenchmarks(): std.unittest.BenchReport { /* compiled code */ }

    func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult { /* compiled code */ }

    func execute(groupName: std.core.String, runConfiguration: std.unittest.common.Configuration, filterService: std.unittest.FilterService): std.unittest.TestSuiteResult { /* compiled code */ }

    static func builder(name: std.core.String): std.unittest.TestSuiteBuilder { /* compiled code */ }

    static func builder(suite: std.unittest.TestSuite): std.unittest.TestSuiteBuilder { /* compiled code */ }

    let _name: std.core.String

    let cases: std.core.Array<std.unittest.CaseOrBench>

    let suiteConfiguration: std.unittest.common.Configuration

    let beforeAlls: std.core.Array<() -> Unit>

    let beforeEachs: std.core.Array<(std.core.String) -> Unit>

    let afterEachs: std.core.Array<(std.core.String) -> Unit>

    let afterAlls: std.core.Array<() -> Unit>

    let template: std.core.Option<std.unittest.TestSuite>

    TestSuite(_name: std.core.String, cases: std.core.Array<std.unittest.CaseOrBench>, suiteConfiguration: std.unittest.common.Configuration, beforeAlls: std.core.Array<() -> Unit>, beforeEachs: std.core.Array<(std.core.String) -> Unit>, afterEachs: std.core.Array<(std.core.String) -> Unit>, afterAlls: std.core.Array<() -> Unit>, template: std.core.Option<std.unittest.TestSuite>) { /* compiled code */ }
}

interface ReportFormat {
    static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    static func fromDefaultConfiguration(): std.core.Option<std.unittest.ReportFormat> { /* compiled code */ }

    abstract func report(testInfo: std.unittest.TestGroupResult): Unit
}

enum BenchReportFormat <: std.unittest.ReportFormat {
    | CSV(std.fs.Path)

    | CSV_RAW(std.fs.Path)

    | Html(std.fs.Path)

    redef static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    redef static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    redef static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    func report(result: std.unittest.TestGroupResult): Unit { /* compiled code */ }
}

enum UTestReportFormat <: std.unittest.ReportFormat {
    | XML(std.fs.Path)

    redef static func default(path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    redef static prop isBench: Bool {
        get() { /* compiled code */ }
    }

    redef static func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat { /* compiled code */ }

    func report(result: std.unittest.TestGroupResult): Unit { /* compiled code */ }
}

struct KeyReportPath <: std.unittest.common.KeyFor<std.core.String> {
    static prop reportPath: std.unittest.KeyReportPath {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

func getReportDirectory(key: Key): std.core.Option<std.fs.Path> { /* compiled code */ }

struct KeyReportFormat <: std.unittest.common.KeyFor<std.core.String> {
    static prop reportFormat: std.unittest.KeyReportFormat {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

class UnittestCliOptionsFormatException <: std.unittest.UnittestException {
    init(optionName: std.core.String, actual: std.core.String, expected: std.core.String) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

interface Result <: std.unittest.common.Serializable<std.unittest.Result> {
    func visitChildren(_: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    func walk(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.Result { /* compiled code */ }

    abstract prop details: std.unittest.Details
}

class RunStepResult <: std.core.Object & std.unittest.Result {
    prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    static func simpleError(step: std.unittest.StepKind, check: std.unittest.CheckResult): std.unittest.RunStepResult { /* compiled code */ }

    func hasFailures(): Bool { /* compiled code */ }

    prop checkResults: std.core.Array<std.unittest.CheckResult> {
        get() { /* compiled code */ }
    }

    func statusCode(): std.unittest.TestCode { /* compiled code */ }

    let checksPassed: Int64

    let startTimestamp: std.time.DateTime

    let kind: std.unittest.StepKind

    let info: std.unittest.StepInfo

    let duration: std.core.Duration

    RunStepResult(checksPassed: Int64, startTimestamp: std.time.DateTime, kind: std.unittest.StepKind, info: std.unittest.StepInfo, duration: std.core.Duration) { /* compiled code */ }
}

class TestCaseResult <: std.unittest.ResultContainer<std.unittest.RunStepResult> {
    static func fromSingleStep(id: std.unittest.TestCaseId, info: std.unittest.TestCaseReportInfo, step: std.unittest.RunStepResult): std.unittest.TestCaseResult { /* compiled code */ }

    var _output: std.unittest.CapturedOutput

    prop output: std.unittest.CapturedOutput {
        get() { /* compiled code */ }
    }

    func addOutput(output: std.unittest.CapturedOutput): Unit { /* compiled code */ }

    func addSuccessfullStep(next: std.unittest.RunStepResult): Unit { /* compiled code */ }

    func hasFailures(): Bool { /* compiled code */ }

    let caseId: std.unittest.TestCaseId

    let caseInfo: std.unittest.TestCaseReportInfo

    let renderOptions: std.unittest.RenderOptions

    TestCaseResult(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, renderOptions: std.unittest.RenderOptions) { /* compiled code */ }
}

class TestGroupResult <: std.unittest.ResultContainer<std.unittest.GroupResultPart> {
    static func fromPackageSuites(packageName: std.core.String, suiteResults: std.core.Collection<std.unittest.TestSuiteResult>, processErrors: std.core.Array<std.unittest.TestProcessError>, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>): std.unittest.TestGroupResult { /* compiled code */ }

    prop hasProcessError: Bool {
        get() { /* compiled code */ }
    }

    func add(testSuite: std.unittest.Result): Unit { /* compiled code */ }

    let groupName: std.core.String

    let nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>

    TestGroupResult(groupName: std.core.String, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>) { /* compiled code */ }
}

class Details {
    var failedCount: Int64

    var passedCount: Int64

    var skippedCount: Int64

    var errorCount: Int64

    var executedSteps: Int64

    var failedSteps: Int64

    var startTimestamp: std.time.DateTime

    var duration: std.core.Duration

    init() { /* compiled code */ }

    func add(other: std.unittest.Details): Unit { /* compiled code */ }

    prop totalCount: Int64 {
        get() { /* compiled code */ }
    }

    prop isSuccess: Bool {
        get() { /* compiled code */ }
    }

    var testcode: std.unittest.TestCode

    var isAccountedStep: Bool

    Details(testcode: std.unittest.TestCode, isAccountedStep: Bool) { /* compiled code */ }
}

class RememberingDataProvider <: std.core.Object & std.unittest.common.DataProvider<T> {
    var info: std.unittest.InputParameter

    var current: std.core.Option<T>

    func provide(): std.core.Iterable<T> { /* compiled code */ }

    let delegate: std.unittest.common.DataProvider<T>

    RememberingDataProvider(name: std.core.String, delegate: std.unittest.common.DataProvider<T>) { /* compiled code */ }
}

struct MeasurementInfo {
    let conversionTable: std.core.Array<(Float64, std.core.String)>

    let name: std.core.String

    let textDescription: std.core.String

    MeasurementInfo(conversionTable: std.core.Array<(Float64, std.core.String)>, name: std.core.String, textDescription: std.core.String) { /* compiled code */ }
}

class TestReport <: std.unittest.Report {
    func reportTo(reporter: std.unittest.Reporter<std.unittest.TestReport, T>): T { /* compiled code */ }

    TestReport(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

class BenchReport <: std.unittest.Report {
    func reportTo(reporter: std.unittest.Reporter<std.unittest.BenchReport, T>): T { /* compiled code */ }

    BenchReport(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

enum ExecutionMode {
    | OnlyTests

    | OnlyBenchmarks

    func asFilter(): std.unittest.FrameworkTestFilter { /* compiled code */ }
}

struct FilterService {
    func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.FilterService { /* compiled code */ }

    let userFilter: std.unittest.TestFilter

    let frameworkFilter: std.unittest.FrameworkTestFilter

    FilterService(userFilter: std.unittest.TestFilter, frameworkFilter: std.unittest.FrameworkTestFilter) { /* compiled code */ }
}

class TestSuiteBuilder {
    let beforeAlls: std.collection.ArrayList<() -> Unit>

    let beforeEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    let afterEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    let afterAlls: std.collection.ArrayList<() -> Unit>

    var configuration: std.unittest.common.Configuration

    var tests: std.collection.ArrayList<std.unittest.CaseOrBench>

    var template_: std.core.Option<std.unittest.TestSuite>

    func setName(name: std.core.String): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func configure(configuration: std.unittest.common.Configuration): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func beforeEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func afterEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func beforeEach(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func afterEach(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func beforeAll(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func afterAll(body: () -> Unit): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func add(test: std.unittest.UnitTestCase): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func add(benchmark: std.unittest.Benchmark): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func template(template: std.unittest.TestSuite): std.unittest.TestSuiteBuilder { /* compiled code */ }

    func build(): std.unittest.TestSuite { /* compiled code */ }

    var name: std.core.String

    TestSuiteBuilder(name: std.core.String) { /* compiled code */ }
}

enum CaseOrBench {
    | Case(std.unittest.UnitTestCase)

    | Bench(std.unittest.Benchmark)

    prop caseConfiguration: std.unittest.common.Configuration {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop isBench: Bool {
        get() { /* compiled code */ }
    }

    prop executor: std.unittest.Executor {
        get() { /* compiled code */ }
    }
}

class TestGroup {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    func runTests(): std.unittest.TestReport { /* compiled code */ }

    func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }

    func runBenchmarks(): std.unittest.BenchReport { /* compiled code */ }

    func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult { /* compiled code */ }

    static func builder(name: std.core.String): std.unittest.TestGroupBuilder { /* compiled code */ }

    static func builder(group: std.unittest.TestGroup): std.unittest.TestGroupBuilder { /* compiled code */ }

    let _name: std.core.String

    let suites: std.core.Array<std.unittest.TestSuite>

    let groupConfiguration: std.unittest.common.Configuration

    TestGroup(_name: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, groupConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

class TestGroupBuilder {
    let suites: std.collection.ArrayList<std.unittest.TestSuite>

    var configuration: std.unittest.common.Configuration

    func setName(name: std.core.String): std.unittest.TestGroupBuilder { /* compiled code */ }

    func configure(configuration: std.unittest.common.Configuration): std.unittest.TestGroupBuilder { /* compiled code */ }

    func add(test: std.unittest.UnitTestCase): std.unittest.TestGroupBuilder { /* compiled code */ }

    func add(benchmark: std.unittest.Benchmark): std.unittest.TestGroupBuilder { /* compiled code */ }

    func add(suite: std.unittest.TestSuite): std.unittest.TestGroupBuilder { /* compiled code */ }

    func build(): std.unittest.TestGroup { /* compiled code */ }

    var name: std.core.String

    TestGroupBuilder(name: std.core.String) { /* compiled code */ }
}

class UnitTestCase {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    func run(): std.unittest.TestReport { /* compiled code */ }

    static func createParameterized(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration: std.unittest.common.Configuration, body: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    static func createParameterized(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, body: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    static func create(name: std.core.String, configuration: std.unittest.common.Configuration, body: () -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    let _name: std.core.String

    let caseConfiguration: std.unittest.common.Configuration

    let executor: std.unittest.Executor

    UnitTestCase(_name: std.core.String, caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

class Benchmark {
    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    func run(): std.unittest.BenchReport { /* compiled code */ }

    static func createWithExplicitLoop(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, optLambda: (T, Int64, Int64) -> Float64): std.unittest.Benchmark { /* compiled code */ }

    static func createParameterized(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: (T) -> Unit): std.unittest.Benchmark { /* compiled code */ }

    static func createParameterized(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: (T) -> Unit): std.unittest.Benchmark { /* compiled code */ }

    static func create(name: std.core.String, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: () -> Unit): std.unittest.Benchmark { /* compiled code */ }

    let _name: std.core.String

    let benchmarkConfiguration: std.unittest.common.Configuration

    let executor: std.unittest.Executor

    Benchmark(_name: std.core.String, benchmarkConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

class DataStrategyProcessor {
    var genIterator: std.core.Iterator<T>

    var currentReduction: Int64

    abstract prop isInfinite: Bool

    abstract func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T>

    abstract func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T>

    abstract func lastItemInfo(): std.core.Array<std.unittest.InputParameter>

    abstract func lastItem(configuration: std.unittest.common.Configuration): T

    func nextIteration(configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    func processNextWith(_: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.unittest.ProcessorResult { /* compiled code */ }

    func shrink(configuration: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.core.Option<std.unittest.ProcessorResult> { /* compiled code */ }

    func intoBenchmark(caseName: std.core.String, configuration: std.unittest.common.Configuration, doRun: (T, Int64, Int64) -> Float64): std.unittest.Benchmark { /* compiled code */ }

    func intoUnitTestCase(caseName: std.core.String, configuration: std.unittest.common.Configuration, doRun: (T) -> Unit): std.unittest.UnitTestCase { /* compiled code */ }

    static func start(s: std.unittest.common.DataStrategy<T>, name: std.core.String): std.unittest.SimpleProcessor<T> { /* compiled code */ }

    static func start(f: () -> std.unittest.common.DataStrategy<U>, name: std.core.String): std.unittest.DataStrategyProcessor<U> { /* compiled code */ }

    static func start(f: () -> std.unittest.common.DataStrategy<T>, name: std.core.String, x: Int64): std.unittest.SimpleProcessor<T> { /* compiled code */ }

    static func start(f: () -> std.unittest.DataStrategyProcessor<T>, _: std.core.String): std.unittest.DataStrategyProcessor<T> { /* compiled code */ }

    static func start(f: () -> std.unittest.DataStrategyProcessor<U>, _: std.core.String, x: Int64): std.unittest.DataStrategyProcessor<U> { /* compiled code */ }

    DataStrategyProcessor() { /* compiled code */ }
}

interface Executor {
    abstract func execute(suiteInfo: std.unittest.TestSuiteReportInfo, caseResult: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit
}

interface Measurement {
    func setup(): Unit { /* compiled code */ }

    func measure(): Float64 { /* compiled code */ }

    prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }

    prop info: std.unittest.MeasurementInfo {
        get() { /* compiled code */ }
    }
}

struct TestCaseInfo {
    let groupName: std.core.String

    let suiteName: std.core.String

    let caseName: std.core.String

    TestCaseInfo(groupName: std.core.String, suiteName: std.core.String, caseName: std.core.String) { /* compiled code */ }
}

class Report {
    let details: std.unittest.Details

    prop errorCount: Int64 {
        get() { /* compiled code */ }
    }

    prop caseCount: Int64 {
        get() { /* compiled code */ }
    }

    prop passedCount: Int64 {
        get() { /* compiled code */ }
    }

    prop failedCount: Int64 {
        get() { /* compiled code */ }
    }

    prop skippedCount: Int64 {
        get() { /* compiled code */ }
    }

    let groupResult: std.unittest.TestGroupResult

    let runConfiguration: std.unittest.common.Configuration

    Report(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

interface Reporter {
}

interface TestClass {
    abstract func asTestSuite(): std.unittest.TestSuite
}

class TestPackage {
    let builder: std.unittest.TestGroupBuilder

    func registerCase(testCase: () -> std.unittest.UnitTestCase): Unit { /* compiled code */ }

    func registerSuite(suite: () -> std.unittest.TestSuite): Unit { /* compiled code */ }

    func registerBench(bench: () -> std.unittest.Benchmark): Unit { /* compiled code */ }

    func build(): std.unittest.TestGroup { /* compiled code */ }

    let name: std.core.String

    TestPackage(name: std.core.String) { /* compiled code */ }
}

interface FrameworkTestFilter <: std.unittest.TestFilter {
    func register(_: std.unittest.SuiteFilterKey, _: std.unittest.TestSuite): Unit { /* compiled code */ }
}

extend TestGroupResult {
    func asBenchReport(runConfiguration: std.unittest.common.Configuration): std.unittest.BenchReport { /* compiled code */ }

    func asTestReport(runConfiguration: std.unittest.common.Configuration): std.unittest.TestReport { /* compiled code */ }
}

func assertEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

class AssertionCtx {
    let _stringArgs: std.core.String

    var _argsMap: std.core.Option<std.collection.HashMap<std.core.String, std.core.String>>

    let _errors: std.collection.ArrayList<std.unittest.PrettyTreeStruct>

    prop caller: std.core.String {
        get() { /* compiled code */ }
    }

    prop hasErrors: Bool {
        get() { /* compiled code */ }
    }

    func arg(key: std.core.String): std.core.String { /* compiled code */ }

    prop args: std.core.String {
        get() { /* compiled code */ }
    }

    func setArgsAliases(aliases: std.core.Array<std.core.String>): Unit { /* compiled code */ }

    func fail(message: std.core.String): Nothing { /* compiled code */ }

    func fail(pt: PP): Nothing { /* compiled code */ }

    func failExpect(message: std.core.String): Unit { /* compiled code */ }

    func failExpect(pt: PP): Unit { /* compiled code */ }

    let _passedArgs: std.core.Array<std.core.String>

    let _caller: std.core.String

    AssertionCtx(_passedArgs: std.core.Array<std.core.String>, _caller: std.core.String) { /* compiled code */ }
}

func assertEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta: Bool, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

func expectEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

func expectEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta: Bool, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

func buildCheckResult(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, checkResultKind: std.unittest.CheckResultKind, isDelta: Bool): std.unittest.AssertExpectCheckResult { /* compiled code */ }

enum CheckResultKind <: std.unittest.common.Serializable<std.unittest.CheckResultKind> {
    | Assert

    | Expect

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResultKind { /* compiled code */ }
}

class AssertExpectCheckResult <: std.unittest.CheckResult {
    let kind: std.unittest.CheckResultKind

    AssertExpectCheckResult(kind: std.unittest.CheckResultKind) { /* compiled code */ }
}

class AssertException <: std.core.Exception {
    let cause: std.core.Option<std.unittest.CheckResult>

    init() { /* compiled code */ }

    init(message: std.core.String, cause: std.unittest.CheckResult) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class CheckResult <: std.core.Object & std.unittest.common.Serializable<std.unittest.CheckResult> {
    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResult { /* compiled code */ }

    init() { /* compiled code */ }
}

class AssertIntermediateException <: std.core.Exception {
    let diagram: std.unittest.PowerAssertDiagram

    let expression: std.core.String

    let originalException: std.core.Exception

    init(exception: std.core.Exception, expression: std.core.String, message: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }

    func getOriginalStackTrace(): std.core.String { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class PowerAssertDiagram <: std.core.Object & std.core.ToString & std.unittest.common.Serializable<std.unittest.PowerAssertDiagram> & std.unittest.common.PrettyPrintable {
    let diagram: std.collection.ArrayList<std.collection.ArrayList<std.unittest.PowerAssertValue>>

    var offset: Int64

    init() { /* compiled code */ }

    func setOffset(offset: Int64): Unit { /* compiled code */ }

    func addValue(newValue: std.unittest.PowerAssertValue): Unit { /* compiled code */ }

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertDiagram { /* compiled code */ }

    func pprintExpression(expr: std.core.String, pp: std.unittest.common.PrettyPrinter): std.core.Any { /* compiled code */ }
}

interface BenchmarkInputMarker {
}

struct SimpleBenchWrapper <: std.unittest.BenchmarkWrapper {
    func measureLoopOnce(times: Int64, max: Int64): Float64 { /* compiled code */ }

    func timeLoopOnce(times: Int64, max: Int64): Float64 { /* compiled code */ }

    let input: T

    let f: (T, Int64, Int64) -> Float64

    SimpleBenchWrapper(input: T, f: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

interface BenchmarkWrapper {
    abstract func measureLoopOnce(times: Int64, max: Int64): Float64

    abstract func timeLoopOnce(times: Int64, max: Int64): Float64
}

interface BenchmarkConfig {
    abstract func batchSize(b: Int64): Unit

    abstract func batchSize(x: std.core.Range<Int64>): Unit

    abstract func warmup(x: Int64): Unit

    abstract func warmup(x: std.core.Duration): Unit

    abstract func minDuration(x: std.core.Duration): Unit

    abstract func explicitGC(x: std.unittest.ExplicitGcType): Unit

    abstract func minBatches(x: Int64): Unit
}

enum ExplicitGcType <: std.core.ToString {
    | Disabled

    | Light

    | Heavy

    func invoke(): Unit { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

extend Configuration <: BenchmarkConfig {
    func batchSize(b: Int64): Unit { /* compiled code */ }

    func batchSize(x: std.core.Range<Int64>): Unit { /* compiled code */ }

    func warmup(x: Int64): Unit { /* compiled code */ }

    func warmup(x: std.core.Duration): Unit { /* compiled code */ }

    func minDuration(x: std.core.Duration): Unit { /* compiled code */ }

    func explicitGC(x: std.unittest.ExplicitGcType): Unit { /* compiled code */ }

    func minBatches(x: Int64): Unit { /* compiled code */ }
}

type BenchRawMeasurement = (Float64, Float64)

class BenchExecutor <: std.core.Object & std.unittest.Executor {
    var step: Int64

    func execute(suiteInfo: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    func runSingle(value: T, inputs: std.core.Array<std.unittest.InputParameter>, suiteInfo: std.unittest.TestSuiteReportInfo, caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, benchRunner: std.unittest.BenchRunner, progress: std.unittest.ProgressOutput, configuration: std.unittest.common.Configuration, step: Int64): std.unittest.RunStepResult { /* compiled code */ }

    let strategy: std.unittest.DataStrategyProcessor<T>

    let doRun: (T, Int64, Int64) -> Float64

    BenchExecutor(strategy: std.unittest.DataStrategyProcessor<T>, doRun: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

struct TestSuiteReportInfo {
    prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    let tags: std.core.Array<std.core.String>

    TestSuiteReportInfo(tags: std.core.Array<std.core.String>) { /* compiled code */ }
}

class InputParameter {
    var position: Int64

    var repr: std.core.String

    func update(param: T): Unit { /* compiled code */ }

    let name: std.core.String

    InputParameter(name: std.core.String) { /* compiled code */ }
}

struct TestCaseId <: std.unittest.common.Serializable<std.unittest.TestCaseId> & std.core.Hashable & std.core.Equatable<std.unittest.TestCaseId> & std.core.ToString {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseId { /* compiled code */ }

    operator func ==(other: std.unittest.TestCaseId): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.TestCaseId): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func fqName(): std.core.String { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let suiteId: std.unittest.TestSuiteId

    let caseName: std.core.String

    let isBench: Bool

    TestCaseId(suiteId: std.unittest.TestSuiteId, caseName: std.core.String, isBench: Bool) { /* compiled code */ }
}

struct TestCaseReportInfo {
    static func empty(): std.unittest.TestCaseReportInfo { /* compiled code */ }

    prop isEmpty: Bool {
        get() { /* compiled code */ }
    }

    func merge(suiteInfo: std.unittest.TestSuiteReportInfo): std.unittest.TestCaseReportInfo { /* compiled code */ }

    let tags: std.core.Array<std.core.String>

    TestCaseReportInfo(tags: std.core.Array<std.core.String>) { /* compiled code */ }
}

class BenchRunner {
    var benchmark: std.unittest.BenchmarkWrapper

    var measurements: std.collection.ArrayList<(Float64, Float64)>

    let time: std.unittest.TimeNow

    var explicitGC: std.unittest.ExplicitGcType

    var targetDuration: std.core.Duration

    var minBatches: Int64

    var batchSize: std.core.Range<Int64>

    var overheadPerIteration: Float64

    var overheadPerBatch: Float64

    var random: std.random.Random

    var isRunning: Bool

    var ema0: std.unittest.Ema

    var ema1: std.unittest.Ema

    func runBench(): Unit { /* compiled code */ }

    func runBenchInner(): Unit { /* compiled code */ }

    func calcBatchSize(estimation: Float64): (std.core.Range<Int64>, Int64) { /* compiled code */ }

    let configuration: std.unittest.common.Configuration

    let progress: std.unittest.ProgressOutput

    BenchRunner(configuration: std.unittest.common.Configuration, progress: std.unittest.ProgressOutput) { /* compiled code */ }
}

class ProgressOutput {
    let isVerbose: Bool

    func println(message: () -> std.core.String): Unit { /* compiled code */ }

    func verboseLine(message: std.core.String): Unit { /* compiled code */ }

    func verbose(message: std.core.String): Unit { /* compiled code */ }

    ProgressOutput(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

struct StatisticsSample {
    let _kde: std.collection.ArrayList<(Float64, Float64)>

    init() { /* compiled code */ }

    func append(data: Float64): Unit { /* compiled code */ }

    func finish(): Unit { /* compiled code */ }

    prop mean: Float64 {
        get() { /* compiled code */ }
    }

    prop median: Float64 {
        get() { /* compiled code */ }
    }

    prop lowerBound: Float64 {
        get() { /* compiled code */ }
    }

    prop upperBound: Float64 {
        get() { /* compiled code */ }
    }

    prop kde: std.collection.ArrayList<(Float64, Float64)> {
        get() { /* compiled code */ }
    }

    prop mode: Float64 {
        get() { /* compiled code */ }
    }

    func mode2(): Float64 { /* compiled code */ }

    let sample: std.unittest.Sample

    StatisticsSample(sample: std.unittest.Sample) { /* compiled code */ }
}

class Sample {
    prop size: Int64 {
        get() { /* compiled code */ }
    }

    func mean(): Float64 { /* compiled code */ }

    func percentile(ratio: Float64): Float64 { /* compiled code */ }

    static func create(data: std.collection.ArrayList<Float64>): std.unittest.Sample { /* compiled code */ }

    func kde(kdeThroughput: Float64): std.collection.ArrayList<(Float64, Float64)> { /* compiled code */ }

    let data: std.collection.ArrayList<Float64>

    Sample(data: std.collection.ArrayList<Float64>) { /* compiled code */ }
}

let BOOTSTRAP_ITERATIONS: Int64

class BenchmarkResult {
    var finished: Bool

    var bootstrapped: std.unittest.BootstrapResult

    prop median: Float64 {
        get() { /* compiled code */ }
    }

    prop medianCI99: Float64 {
        get() { /* compiled code */ }
    }

    prop medianOverhead: Float64 {
        get() { /* compiled code */ }
    }

    prop mean: Float64 {
        get() { /* compiled code */ }
    }

    prop meanCI99: Float64 {
        get() { /* compiled code */ }
    }

    prop meanLower: Float64 {
        get() { /* compiled code */ }
    }

    prop meanUpper: Float64 {
        get() { /* compiled code */ }
    }

    prop meanOverhead: Float64 {
        get() { /* compiled code */ }
    }

    prop mainResult: Float64 {
        get() { /* compiled code */ }
    }

    prop errorEst: Float64 {
        get() { /* compiled code */ }
    }

    func calculate(): Unit { /* compiled code */ }

    func valuesSample(): std.unittest.StatisticsSample { /* compiled code */ }

    var data: std.collection.ArrayList<(Float64, Float64)>

    BenchmarkResult(data: std.collection.ArrayList<(Float64, Float64)>) { /* compiled code */ }
}

struct BootstrapResult {
    let meanStats: std.unittest.StatisticsSample

    let meanOverheadStats: std.unittest.StatisticsSample

    let medianStats: std.unittest.StatisticsSample

    let medianOverheadStats: std.unittest.StatisticsSample

    let r2Stats: std.unittest.StatisticsSample

    let buffer: std.collection.ArrayList<Float64>

    func join(other: std.unittest.BootstrapResult): std.unittest.BootstrapResult { /* compiled code */ }

    func finish(): Unit { /* compiled code */ }

    func appendStatisticsFromSample(subsample: std.collection.ArrayList<(Float64, Float64)>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

func percentToColor(p: Float64): std.unittest.common.Color { /* compiled code */ }

let PLUS_MINUS: std.core.String

enum Baseline {
    | PreviousReport

    | TestCaseBaseline(Float64)

    | NotFound
}

class BenchTableProperties {
    var hasArgsColumn: Bool

    var hasCasesBaseline: Bool

    var hasMeasurementColumn: Bool

    var hasRatioColumn: Bool

    var shouldPrintMeasurement: Bool

    var conversionTable: std.core.Array<(Float64, std.core.String)>

    var measurementName: std.core.String

    var baseline: std.unittest.Baseline

    let baselineReport: std.collection.HashMap<std.core.String, Float64>

    BenchTableProperties(hasArgsColumn: Bool, hasCasesBaseline: Bool, hasMeasurementColumn: Bool, hasRatioColumn: Bool, shouldPrintMeasurement: Bool, conversionTable: std.core.Array<(Float64, std.core.String)>, measurementName: std.core.String, baseline: std.unittest.Baseline, baselineReport: std.collection.HashMap<std.core.String, Float64>) { /* compiled code */ }
}

class PrettyTable <: std.core.Object & std.unittest.common.PrettyPrintable {
    let columnSizes: std.collection.ArrayList<Int64>

    let rows: std.collection.ArrayList<std.collection.ArrayList<(std.core.String, std.unittest.common.Color)>>

    var column: Int64

    let leftAlignedColumns: std.collection.HashSet<Int64>

    init() { /* compiled code */ }

    func addColumns(names: T): Unit { /* compiled code */ }

    func addColumn(name: std.core.String): Unit { /* compiled code */ }

    func removeLastColumn(): (std.core.String, std.unittest.common.Color) { /* compiled code */ }

    func alignColumnLeft(idx: Int64): Bool { /* compiled code */ }

    func addEmptyRow(): Unit { /* compiled code */ }

    func nextRow(): Unit { /* compiled code */ }

    func removeCurrentRow(): std.collection.ArrayList<(std.core.String, std.unittest.common.Color)> { /* compiled code */ }

    func addCell(value: std.core.String, color: std.unittest.common.Color): Unit { /* compiled code */ }

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    let names: std.collection.ArrayList<std.core.String>

    PrettyTable(names: std.collection.ArrayList<std.core.String>) { /* compiled code */ }
}

struct TimeNow <: std.unittest.Measurement {
    var unit: std.core.Option<std.unittest.TimeUnit>

    init(unit: std.core.Option<std.unittest.TimeUnit>) { /* compiled code */ }

    init() { /* compiled code */ }

    func measure(): Float64 { /* compiled code */ }

    prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }
}

struct Ema {
    var result: Float64

    var currentLength: Float64

    mut func reset(): Unit { /* compiled code */ }

    func reachedMax(): Bool { /* compiled code */ }

    mut func append(next: Float64): Unit { /* compiled code */ }

    let maxLength: Int64

    Ema(maxLength: Int64) { /* compiled code */ }
}

type MeasurementUnitTable = std.core.Array<(Float64, std.core.String)>

let RADIX_UNIT_TABLE: std.core.Array<(Float64, std.core.String)>

func printDuration(dur: std.core.Duration): std.core.String { /* compiled code */ }

enum TimeUnit <: std.core.ToString {
    | Nanos

    | Micros

    | Millis

    | Seconds

    func toString(): std.core.String { /* compiled code */ }

    func inNanos(): Float64 { /* compiled code */ }

    func conversionRow(): (Float64, std.core.String) { /* compiled code */ }
}

foreign func GetRdtsc(): UInt64

struct CpuCycles <: std.unittest.Measurement {
    func measure(): Float64 { /* compiled code */ }

    func setup(): Unit { /* compiled code */ }

    prop conversionTable: std.core.Array<(Float64, std.core.String)> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop textDescription: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

class UnattachedCheckResult <: std.unittest.CheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.UnattachedCheckResult> { /* compiled code */ }

    let inner: std.unittest.CheckResult

    UnattachedCheckResult(inner: std.unittest.CheckResult) { /* compiled code */ }
}

class ErrorCheckResult <: std.unittest.CheckResult {
    init() { /* compiled code */ }
}

class PlainCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainCompareCheckResult> { /* compiled code */ }

    let leftExpr: std.core.String

    let rightExpr: std.core.String

    let leftValue: std.core.String

    let rightValue: std.core.String

    PlainCompareCheckResult(kind: std.unittest.CheckResultKind, leftExpr: std.core.String, rightExpr: std.core.String, leftValue: std.core.String, rightValue: std.core.String) { /* compiled code */ }
}

class PlainDeltaCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainDeltaCompareCheckResult> { /* compiled code */ }

    let expr: std.core.String

    PlainDeltaCompareCheckResult(kind: std.unittest.CheckResultKind, expr: std.core.String) { /* compiled code */ }
}

class FailCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.FailCheckResult> { /* compiled code */ }

    let failMessage: std.core.String

    FailCheckResult(kind: std.unittest.CheckResultKind, failMessage: std.core.String) { /* compiled code */ }
}

class AssertThrowsCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.AssertThrowsCheckResult> { /* compiled code */ }

    let message: std.core.String

    let expectedExceptions: std.core.String

    let caughtException: std.core.String

    AssertThrowsCheckResult(kind: std.unittest.CheckResultKind, message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String) { /* compiled code */ }
}

class PowerAssertCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertCheckResult> { /* compiled code */ }

    let expr: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    PowerAssertCheckResult(expr: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

class PowerAssertExceptionCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertExceptionCheckResult> { /* compiled code */ }

    let expression: std.core.String

    let exception: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    PowerAssertExceptionCheckResult(expression: std.core.String, exception: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

class DiffCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult> { /* compiled code */ }

    let diffMessage: std.unittest.common.PrettyText

    DiffCheckResult(kind: std.unittest.CheckResultKind, diffMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

class CustomCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult> { /* compiled code */ }

    let custom: std.unittest.common.PrettyText

    CustomCheckResult(kind: std.unittest.CheckResultKind, custom: std.unittest.common.PrettyText) { /* compiled code */ }
}

class ExceptionThrownCheckResult <: std.unittest.ErrorCheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.ExceptionThrownCheckResult> { /* compiled code */ }

    let errorMessage: std.unittest.common.PrettyText

    let stackTrace: std.unittest.common.PrettyText

    ExceptionThrownCheckResult(errorMessage: std.unittest.common.PrettyText, stackTrace: std.unittest.common.PrettyText) { /* compiled code */ }
}

class MockFrameworkCheckResult <: std.unittest.CheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.MockFrameworkCheckResult> { /* compiled code */ }

    let mockFrameworkMessage: std.unittest.common.PrettyText

    MockFrameworkCheckResult(mockFrameworkMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

class NoParametersProvidedResult <: std.unittest.CheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.NoParametersProvidedResult> { /* compiled code */ }

    NoParametersProvidedResult() { /* compiled code */ }
}

class CrashedCheckResult <: std.unittest.CheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.CrashedCheckResult> { /* compiled code */ }

    let exitCode: Int64

    CrashedCheckResult(exitCode: Int64) { /* compiled code */ }
}

class TimeoutCheckResult <: std.unittest.CheckResult {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.TimeoutCheckResult> { /* compiled code */ }

    let msg: std.unittest.common.PrettyText

    TimeoutCheckResult(msg: std.unittest.common.PrettyText) { /* compiled code */ }
}

enum PrettyTreeStruct <: std.unittest.common.PrettyPrintable {
    | Leaf(std.unittest.AssertExpectCheckResult)

    | Node(std.core.String, std.collection.ArrayList<std.unittest.PrettyTreeStruct>)

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

func invokeCustomAssert(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, assert: (std.unittest.AssertionCtx) -> T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): T { /* compiled code */ }

func invokeCustomExpect(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, expect: (std.unittest.AssertionCtx) -> std.core.Any, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

func fillAbsentValues(config: std.unittest.common.Configuration): Unit { /* compiled code */ }

func kebabCaseToCamelCase(str: std.core.String): std.core.String { /* compiled code */ }

func camelCaseToKebabCase(str: std.core.String): std.core.String { /* compiled code */ }

func setValueWithGuessedType(configuration: std.unittest.common.Configuration, key: std.core.String, value: std.core.String): Unit { /* compiled code */ }

func buildDefaultConfiguration(): std.unittest.common.Configuration { /* compiled code */ }

var defaultConfigurationCached: std.sync.AtomicOptionReference<std.unittest.common.Configuration>

func setValuesFromJsonConfiguration(configuration: std.unittest.common.Configuration, rawJson: std.core.String): Unit { /* compiled code */ }

func typeValidator(name: std.core.String): (std.core.Any) -> std.unittest.common.OptionValidity { /* compiled code */ }

func registerUnittestOption(key: std.unittest.common.KeyFor<T>, description: std.core.Option<std.core.String>, typeDescription: std.core.Option<(std.core.String, std.core.String)>): Unit { /* compiled code */ }

interface NearEquatable {
    abstract func isNear(obj: CT, delta: D): Bool
}

struct RelativeDelta {
    let absolute: T

    let relative: T

    RelativeDelta(absolute: T, relative: T) { /* compiled code */ }
}

func isNearExpansion(l: CT, r: CT, delta: D, cmpType: std.core.String): Bool { /* compiled code */ }

func isNearExpansion(l: CT, r: CT, delta: D, cmpType: std.core.String, overloadHack: Bool): Bool { /* compiled code */ }

extend `Unit` <: NearEquatable & NearEquatable {
    func isNear(obj: Float16, delta: Float16): Bool { /* compiled code */ }

    func isNear(obj: Float16, delta: std.unittest.RelativeDelta<Float16>): Bool { /* compiled code */ }
}

extend `Unit` <: NearEquatable & NearEquatable {
    func isNear(obj: Float32, delta: Float32): Bool { /* compiled code */ }

    func isNear(obj: Float32, delta: std.unittest.RelativeDelta<Float32>): Bool { /* compiled code */ }
}

extend `Unit` <: NearEquatable & NearEquatable {
    func isNear(obj: Float64, delta: Float64): Bool { /* compiled code */ }

    func isNear(obj: Float64, delta: std.unittest.RelativeDelta<Float64>): Bool { /* compiled code */ }
}

func entryMain(testPackage: std.unittest.TestPackage): Int64 { /* compiled code */ }

func testRunnerEntryMain(): Int64 { /* compiled code */ }

class ModuleExecutionResult <: std.unittest.ResultContainer<std.unittest.TestGroupResult> {
    let moduleName: std.core.String

    ModuleExecutionResult(moduleName: std.core.String) { /* compiled code */ }
}

class ResultContainer <: std.core.Object & std.unittest.Result {
    let subResults: std.collection.ArrayList<T>

    func visitChildren(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    func add(child: T): Unit { /* compiled code */ }

    var _details: std.unittest.Details

    var _detailsComputed: Bool

    var finished: Bool

    prop isFinished: Bool {
        get() { /* compiled code */ }
    }

    prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    func finish(): Unit { /* compiled code */ }

    func visitAll(visitor: (V) -> Unit): Unit { /* compiled code */ }

    func visitPassedTests(visitor: (std.unittest.TestCaseResult) -> Unit): Unit { /* compiled code */ }

    func visitPassedBenches(visitor: (std.unittest.BenchStepEntry) -> Unit): Unit { /* compiled code */ }

    func visitFailedSteps(visitor: (std.unittest.RunStepResult, std.unittest.TestCaseResult) -> Unit): Unit { /* compiled code */ }

    func serializeSubResults(): std.unittest.common.DataModelStruct { /* compiled code */ }

    init() { /* compiled code */ }
}

class ProjectExecutionResult <: std.unittest.ResultContainer<std.unittest.ModuleExecutionResult> {
    init() { /* compiled code */ }
}

class PackageExecutionResult {
    let packageResult: std.unittest.TestGroupResult

    PackageExecutionResult(packageResult: std.unittest.TestGroupResult) { /* compiled code */ }
}

struct TestPackageExecuteCommand <: std.unittest.common.Serializable<std.unittest.TestPackageExecuteCommand> {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    func overridingCurrentEnv(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    func withJsonConfiguration(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    func withRunnerOption(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    func withArgs(newArgs: std.core.Array<std.core.String>): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    static func fromCurrentProcess(): std.unittest.TestPackageExecuteCommand { /* compiled code */ }

    let command: std.core.String

    let args: std.core.Array<std.core.String>

    let env: std.collection.Map<std.core.String, std.core.String>

    TestPackageExecuteCommand(command: std.core.String, args: std.core.Array<std.core.String>, env: std.collection.Map<std.core.String, std.core.String>) { /* compiled code */ }
}

struct ParallelCtx {
    let nWorkersPerPackage: Int64

    let workersQuota: std.sync.Semaphore

    ParallelCtx(nWorkersPerPackage: Int64, workersQuota: std.sync.Semaphore) { /* compiled code */ }
}

struct ReportCtx {
    let perPackageReporter: std.unittest.PerPackageReporter

    let outputReporter: std.unittest.TestOutputReporter

    ReportCtx(perPackageReporter: std.unittest.PerPackageReporter, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

class PerPackageReporter {
    func printIntermediateResult(executionResult: std.unittest.PackageExecutionResult): Unit { /* compiled code */ }

    func dumpPackageReport(executionResult: std.unittest.PackageExecutionResult): std.core.Option<Unit> { /* compiled code */ }

    let format: std.core.Option<std.unittest.ReportFormat>

    let outputReporter: std.unittest.TestOutputReporter

    PerPackageReporter(format: std.core.Option<std.unittest.ReportFormat>, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

class TestOutputReporter {
    func report(pp: std.unittest.common.PrettyPrinter, prefix: std.unittest.common.PrettyText, output: std.unittest.CapturedOutput, success: Bool): std.unittest.common.PrettyPrinter { /* compiled code */ }

    static func fromConfiguration(conf: std.unittest.common.Configuration): std.unittest.TestOutputReporter { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.TestOutputReporter { /* compiled code */ }

    let capture: Bool

    let showAllOutput: Bool

    let verbose: Bool
}

let EXIT_CODE_ON_TIMEOUT: Int64

struct MainExecutionCtx {
    func withCommand(command: std.unittest.TestPackageExecuteCommand): std.unittest.MainExecutionCtx { /* compiled code */ }

    let nWorkers: Int64

    let workersQuota: std.sync.Semaphore

    let outputReporter: std.unittest.TestOutputReporter

    let executeCommand: std.unittest.TestPackageExecuteCommand

    MainExecutionCtx(nWorkers: Int64, workersQuota: std.sync.Semaphore, outputReporter: std.unittest.TestOutputReporter, executeCommand: std.unittest.TestPackageExecuteCommand) { /* compiled code */ }
}

func executeSmart(packageName: std.core.String, tests: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService, outputReporter: std.unittest.TestOutputReporter, needStartWorker: Bool): std.unittest.TestGroupResult { /* compiled code */ }

func initWorkersMain(ctx: std.unittest.MainExecutionCtx): std.core.Array<std.unittest.WorkerProcess> { /* compiled code */ }

class WorkerProcess {
    func waitCasesRegistration(): std.core.Option<Bool> { /* compiled code */ }

    func commitCasesRegistrationFailure(): std.core.Option<Bool> { /* compiled code */ }

    static func create(ctx: std.unittest.MainExecutionCtx, workerId: Int64, progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>, connect: () -> std.unittest.TransportChannel): std.unittest.WorkerProcess { /* compiled code */ }

    static func createAndRegister(workerId: Int64, ctx: std.unittest.MainExecutionCtx): std.unittest.WorkerProcess { /* compiled code */ }

    static func executeRestarting(initialWorker: std.unittest.WorkerProcess, ctx: std.unittest.MainExecutionCtx): std.unittest.ExecuteRestartingResult { /* compiled code */ }

    let process: std.unittest.WrappedProcess

    let transportChannel: std.core.Option<std.unittest.TransportChannel>

    let progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    let outputReporter: std.unittest.TestOutputReporter

    let workerId: Int64
}

func executeMain(workers: std.core.Array<std.unittest.WorkerProcess>, packageName: std.core.String, ctx: std.unittest.MainExecutionCtx): std.unittest.TestGroupResult { /* compiled code */ }

func uniqueValues(collection: std.core.Collection<T>): std.core.Array<T> { /* compiled code */ }

func executeWorker(packageName: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService): std.unittest.TestGroupResult { /* compiled code */ }

enum TestProcessKind {
    | Main

    | Worker(Int64, Int64, Int64, UInt16)

    static func fromDefaultConfiguration(): std.unittest.TestProcessKind { /* compiled code */ }

    prop isWorker: Bool {
        get() { /* compiled code */ }
    }
}

struct NonTestOutputs <: std.core.Hashable {
    func hashCode(): Int64 { /* compiled code */ }

    init() { /* compiled code */ }

    init(pair: (std.unittest.CapturedOutput, std.unittest.CapturedOutput)) { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    let outputBeforeTests: std.unittest.CapturedOutput

    let outputAfterTests: std.unittest.CapturedOutput

    NonTestOutputs(outputBeforeTests: std.unittest.CapturedOutput, outputAfterTests: std.unittest.CapturedOutput) { /* compiled code */ }
}

struct CapturedOutput {
    init() { /* compiled code */ }

    init(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }

    func isEmpty(): Bool { /* compiled code */ }

    let stdout: std.core.String

    let stderr: std.core.String

    CapturedOutput(stdout: std.core.String, stderr: std.core.String) { /* compiled code */ }
}

enum ExecuteRestartingResult {
    | OkResult(std.core.Collection<std.unittest.TestSuiteResult>, std.collection.ArrayList<std.unittest.NonTestOutputs>)

    | CrashResult(std.core.Collection<std.unittest.TestSuiteResult>, std.unittest.TestProcessError, std.collection.ArrayList<std.unittest.NonTestOutputs>)
}

class TestProcessError <: std.core.Object & std.unittest.Result & std.core.Hashable {
    let _details: std.unittest.Details

    func hashCode(): Int64 { /* compiled code */ }

    prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }

    let code: Int64

    TestProcessError(code: Int64) { /* compiled code */ }
}

interface UTProgress {
}

class TransportChannel <: std.core.Object & std.core.Resource {
    prop port: UInt16 {
        get() { /* compiled code */ }
    }

    func isClosed(): Bool { /* compiled code */ }

    func close(): Unit { /* compiled code */ }

    func send(part: std.unittest.ExecutionResultPart): Unit { /* compiled code */ }

    func receive(limit: std.core.Option<UInt64>): std.core.Iterator<std.unittest.ExecutionResultPart> { /* compiled code */ }

    func delimitOutput(): Unit { /* compiled code */ }

    let socket: std.net.TcpSocket

    let outputReporter: std.unittest.TestOutputReporter

    let isMain: Bool

    TransportChannel(socket: std.net.TcpSocket, outputReporter: std.unittest.TestOutputReporter, isMain: Bool) { /* compiled code */ }
}

struct WrappedProcess {
    let status: std.sync.AtomicOptionReference<std.unittest.CapturedProcessResult>

    func wait(): Int64 { /* compiled code */ }

    func terminate(): Unit { /* compiled code */ }

    func stdout(): std.core.Array<UInt8> { /* compiled code */ }

    func stderr(): std.core.Array<UInt8> { /* compiled code */ }

    let process: std.process.SubProcess

    let stdOutFile: std.core.Option<std.fs.Path>

    let stdErrFile: std.core.Option<std.fs.Path>

    WrappedProcess(process: std.process.SubProcess, stdOutFile: std.core.Option<std.fs.Path>, stdErrFile: std.core.Option<std.fs.Path>) { /* compiled code */ }
}

class ExecutionResultPart <: std.core.Object & std.unittest.common.Serializable<std.unittest.ExecutionResultPart> {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.ExecutionResultPart { /* compiled code */ }

    init() { /* compiled code */ }
}

class ExecutionResultHeader <: std.unittest.ExecutionResultPart {
    abstract prop suiteId: std.unittest.TestSuiteId

    abstract prop suiteInfo: std.unittest.TestSuiteReportInfo

    abstract prop startTime: std.time.DateTime

    init() { /* compiled code */ }
}

struct TestSuiteId <: std.unittest.common.Serializable<std.unittest.TestSuiteId> & std.core.Hashable & std.core.Equatable<std.unittest.TestSuiteId> & std.core.ToString {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteId { /* compiled code */ }

    operator func ==(other: std.unittest.TestSuiteId): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.TestSuiteId): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    static func fromTestSuite(groupName: std.core.String, testSuite: std.unittest.TestSuite): std.unittest.TestSuiteId { /* compiled code */ }

    prop isFromTopLevelFunc: Bool {
        get() { /* compiled code */ }
    }

    let groupName: std.core.String

    let suiteName: std.core.String

    TestSuiteId(groupName: std.core.String, suiteName: std.core.String) { /* compiled code */ }
}

class TestCaseExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    override prop suiteId: std.unittest.TestSuiteId {
        get() { /* compiled code */ }
    }

    override prop suiteInfo: std.unittest.TestSuiteReportInfo {
        get() { /* compiled code */ }
    }

    override prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    let caseId: std.unittest.TestCaseId

    let caseInfo: std.unittest.TestCaseReportInfo

    let suiteInfo_: std.unittest.TestSuiteReportInfo

    let startTime_: std.time.DateTime

    TestCaseExecutionResultHeader(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo_: std.unittest.TestSuiteReportInfo, startTime_: std.time.DateTime) { /* compiled code */ }
}

class LifecycleExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    override prop suiteId: std.unittest.TestSuiteId {
        get() { /* compiled code */ }
    }

    override prop suiteInfo: std.unittest.TestSuiteReportInfo {
        get() { /* compiled code */ }
    }

    override prop startTime: std.time.DateTime {
        get() { /* compiled code */ }
    }

    let suiteId_: std.unittest.TestSuiteId

    let suiteInfo_: std.unittest.TestSuiteReportInfo

    let step: std.unittest.LStep

    let startTime_: std.time.DateTime

    LifecycleExecutionResultHeader(suiteId_: std.unittest.TestSuiteId, suiteInfo_: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep, startTime_: std.time.DateTime) { /* compiled code */ }
}

enum LStep <: std.core.ToString {
    | BeforeEach

    | BeforeAll

    | AfterAll

    | AfterEach

    operator func ==(other: std.unittest.LStep): Bool { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class ExecutionResultBody <: std.unittest.ExecutionResultPart {
    let stepResult: std.unittest.TestCaseResult

    ExecutionResultBody(stepResult: std.unittest.TestCaseResult) { /* compiled code */ }
}

class TestCasesRegistrationPart <: std.unittest.ExecutionResultPart {
    let casesCount: UInt64

    let workerId: Int64

    let packageName: std.core.String

    TestCasesRegistrationPart(casesCount: UInt64, workerId: Int64, packageName: std.core.String) { /* compiled code */ }
}

class TestCasesRegistrationFailed <: std.unittest.ExecutionResultPart {
    TestCasesRegistrationFailed() { /* compiled code */ }
}

class ExecutionPermitPart <: std.unittest.ExecutionResultPart {
    ExecutionPermitPart() { /* compiled code */ }
}

func failExpect(message: std.core.String): Unit { /* compiled code */ }

func fail(message: std.core.String): Nothing { /* compiled code */ }

func assertCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Nothing { /* compiled code */ }

func expectCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit { /* compiled code */ }

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

struct CaseFilterKey {
    static func fromTestCase(suiteId: std.unittest.TestSuiteId, cob: std.unittest.CaseOrBench, config: std.core.Option<std.unittest.common.Configuration>): std.unittest.CaseFilterKey { /* compiled code */ }

    let caseId: std.unittest.TestCaseId

    let tags: std.core.Array<std.core.String>

    let isTopLevel: Bool

    CaseFilterKey(caseId: std.unittest.TestCaseId, tags: std.core.Array<std.core.String>, isTopLevel: Bool) { /* compiled code */ }
}

class BenchTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    override func shouldSkipTestClass(_: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.BenchTestFilter { /* compiled code */ }

    let benchEnabled: Bool

    BenchTestFilter(benchEnabled: Bool) { /* compiled code */ }
}

struct FilterOption {
    let ACCEPTING: std.unittest.FilterOption

    static func fromString(str: std.core.String): std.unittest.FilterOption { /* compiled code */ }

    prop classFilter: std.core.String {
        get() { /* compiled code */ }
    }

    prop funcFilter: std.core.String {
        get() { /* compiled code */ }
    }

    let _classRegex: std.core.String

    let _funcRegex: std.core.String

    let exclude: Bool

    let isFuncFilter: Bool

    FilterOption(_classRegex: std.core.String, _funcRegex: std.core.String, exclude: Bool, isFuncFilter: Bool) { /* compiled code */ }
}

class TagsFilterOption {
    static func fromString(input: std.core.String): std.unittest.TagsFilterOption { /* compiled code */ }

    let tags: std.collection.HashSet<std.core.String>

    TagsFilterOption(tags: std.collection.HashSet<std.core.String>) { /* compiled code */ }
}

class WorkerTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    let shouldSkipClassCache: std.collection.HashMap<std.unittest.TestSuiteId, Bool>

    let shouldSkipCaseCache: std.collection.HashMap<std.unittest.TestCaseId, Bool>

    var testEntityIdx: Int64

    var testCaseIdx: Int64

    override func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit { /* compiled code */ }

    override func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool { /* compiled code */ }

    override func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool { /* compiled code */ }

    let defaultFilter: std.unittest.FrameworkTestFilter

    let workerId: Int64

    let nWorkers: Int64

    let nCasesSkip: Int64

    WorkerTestFilter(defaultFilter: std.unittest.FrameworkTestFilter, workerId: Int64, nWorkers: Int64, nCasesSkip: Int64) { /* compiled code */ }
}

enum LaunchApi {
    | FromPublicApi

    | FromCli
}

enum LaunchSource {
    | TestBinary

    | TestRunner

    static func fromDefaultConfiguration(): std.unittest.LaunchSource { /* compiled code */ }
}

class Framework {
    var transport: std.core.Option<std.unittest.Transport>

    var progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    let mtx: std.sync.ReentrantMutex

    var currentlyRunning: std.unittest.RunContext

    static func launch(api: std.unittest.LaunchApi, hasWorkersInSetup: Bool, progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>, body: () -> T): T { /* compiled code */ }

    static func isExecuting(): Bool { /* compiled code */ }

    static func runStepBody(stepKind: std.unittest.StepKind, info: std.unittest.StepInfo, body: () -> Unit): std.unittest.RunStepResult { /* compiled code */ }

    static func runCatching(f: () -> Unit): Unit { /* compiled code */ }

    static func withCurrentContext(f: (std.unittest.RunContext) -> T): T { /* compiled code */ }

    static func abortCurrentStep(): std.core.Option<std.unittest.RunStepResult> { /* compiled code */ }

    static func collectUnattachedFailures(): std.collection.ArrayList<std.unittest.CheckResult> { /* compiled code */ }

    static func onTestCasesRegistered(packageName: std.core.String, casesCount: UInt64): Unit { /* compiled code */ }

    static func onLifecycleStart(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep): Unit { /* compiled code */ }

    static func onTestCaseStarted(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo: std.unittest.TestSuiteReportInfo): Unit { /* compiled code */ }

    static func onFinished(result: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    static func initWorker(ctx: std.unittest.MainExecutionCtx, workerId: Int64): std.unittest.WorkerProcess { /* compiled code */ }
}

enum Transport <: std.core.Resource {
    | TChannel(std.unittest.TransportChannel)

    | TMain(std.unittest.TransportMain)

    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    prop channel: std.core.Option<std.unittest.TransportChannel> {
        get() { /* compiled code */ }
    }

    prop manager: std.core.Option<std.unittest.TransportMain> {
        get() { /* compiled code */ }
    }

    static func fromDefaultConfiguration(): std.unittest.Transport { /* compiled code */ }
}

class RunContext {
    let checkResults: std.collection.ArrayList<std.unittest.CheckResult>

    var suppressed: Bool

    var passedChecks: Int64

    func checkFailed(c: std.unittest.CheckResult): Unit { /* compiled code */ }

    func onException(e: std.core.Exception): Unit { /* compiled code */ }

    func storeCheckResult(check: std.unittest.CheckResult): Unit { /* compiled code */ }

    func checkPassed(): Unit { /* compiled code */ }

    func suppressIfHasFailures(): Unit { /* compiled code */ }

    func hasFailures(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

enum StepKind {
    | Skip

    | NoRun

    | Lifecycle(std.unittest.LStep)

    | CaseStep(std.unittest.ArgumentDescription)

    | UserCode

    func isClassLifecycle(): Bool { /* compiled code */ }
}

enum StepInfo {
    | Test(Int64)

    | Bench(std.unittest.BenchmarkResult)

    | Failure(std.core.Array<std.unittest.CheckResult>)
}

class OutsideOfFrameworkContext <: std.unittest.RunContext {
    func storeCheckResult(_: std.unittest.CheckResult): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class FrameworkInternalsContext <: std.unittest.RunContext {
    init() { /* compiled code */ }
}

class RunStepContext <: std.unittest.RunContext {
    var startTime: std.time.DateTime

    func checkFailed(c: std.unittest.CheckResult): Unit { /* compiled code */ }

    func finishStep(): std.unittest.RunStepResult { /* compiled code */ }

    var stepKind: std.unittest.StepKind

    var stepInfo: std.unittest.StepInfo

    RunStepContext(stepKind: std.unittest.StepKind, stepInfo: std.unittest.StepInfo) { /* compiled code */ }
}

class BenchmarkStoppedError <: std.core.Exception {
    BenchmarkStoppedError() { /* compiled code */ }
}

struct UInt8Entry {
    let left: UInt8

    let right: UInt8

    UInt8Entry(left: UInt8, right: UInt8) { /* compiled code */ }
}

struct UInt16Entry {
    let left: UInt16

    let right: UInt16

    UInt16Entry(left: UInt16, right: UInt16) { /* compiled code */ }
}

struct UInt32Entry {
    let left: UInt32

    let right: UInt32

    UInt32Entry(left: UInt32, right: UInt32) { /* compiled code */ }
}

struct UInt64Entry {
    let left: UInt64

    let right: UInt64

    UInt64Entry(left: UInt64, right: UInt64) { /* compiled code */ }
}

struct Float32Entry {
    let left: Float32

    let right: Float32

    Float32Entry(left: Float32, right: Float32) { /* compiled code */ }
}

struct Float64Entry {
    let left: Float64

    let right: Float64

    Float64Entry(left: Float64, right: Float64) { /* compiled code */ }
}

foreign func CJ_MICROFUZZ_FLIP_BIT_FLOAT(value: Float32, bit: UInt8): Float32

foreign func CJ_MICROFUZZ_FLIP_BIT_DOUBLE(value: Float64, bit: UInt8): Float64

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT8(result: CPointer<std.unittest.UInt8Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT16(result: CPointer<std.unittest.UInt16Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT32(result: CPointer<std.unittest.UInt32Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT64(result: CPointer<std.unittest.UInt64Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FLOAT(result: CPointer<std.unittest.Float32Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_DOUBLE(result: CPointer<std.unittest.Float64Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT8(key: UInt8, result: CPointer<std.unittest.UInt8Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT16(key: UInt16, result: CPointer<std.unittest.UInt16Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT32(key: UInt32, result: CPointer<std.unittest.UInt32Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT64(key: UInt64, result: CPointer<std.unittest.UInt64Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_FLOAT(key: Float32, result: CPointer<std.unittest.Float32Entry>): Unit

foreign func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_DOUBLE(key: Float64, result: CPointer<std.unittest.Float64Entry>): Unit

foreign func CJ_MICROFUZZ_ENABLE_TRACE(): Unit

foreign func CJ_MICROFUZZ_DISABLE_TRACE(): Unit

foreign func CJ_MICROFUZZ_RESET_LOCAL_COVERAGE_INFO(): Unit

foreign func CJ_MICROFUZZ_RESET_ALL_COVERAGE_INFO(): Unit

foreign func CJ_MICROFUZZ_HAS_NEW_COVERAGE(): Bool

foreign func CJ_MICROFUZZ_IS_INITIALIZED(): Bool

foreign func CJ_MICROFUZZ_GLOBAL_EDGE_COVERAGE_EST(): Float64

foreign func CJ_MICROFUZZ_SET_DBG_CALLBACK(callback: (CString, CString, UInt64, UInt64) -> Unit): Unit

func printMessage(mes: CString, t: CString, v1: UInt64, v2: UInt64): Unit { /* compiled code */ }

func currentCoverageEst(): Float64 { /* compiled code */ }

interface Torcable {
    abstract static func randomRecentEntry(): std.core.Option<(T, T)>

    abstract static func randomRecentValueComparedTo(value: T): std.core.Option<T>

    abstract static func nextFrom(random: std.random.Random): T

    abstract static func suggestedFrom(random: std.random.Random): T

    abstract func isZero(): Bool

    abstract func bitFlip(at: UInt8): T

    abstract operator func +(_: UInt8): T

    abstract operator func -(_: UInt8): T
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(UInt8, UInt8)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: UInt8): std.core.Option<UInt8> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): UInt8 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): UInt8 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): UInt8 { /* compiled code */ }
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(UInt16, UInt16)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: UInt16): std.core.Option<UInt16> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): UInt16 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): UInt16 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): UInt16 { /* compiled code */ }

    operator func +(arg: UInt8): UInt16 { /* compiled code */ }

    operator func -(arg: UInt8): UInt16 { /* compiled code */ }
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(UInt32, UInt32)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: UInt32): std.core.Option<UInt32> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): UInt32 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): UInt32 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): UInt32 { /* compiled code */ }

    operator func +(arg: UInt8): UInt32 { /* compiled code */ }

    operator func -(arg: UInt8): UInt32 { /* compiled code */ }
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(UInt64, UInt64)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: UInt64): std.core.Option<UInt64> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): UInt64 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): UInt64 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): UInt64 { /* compiled code */ }

    operator func +(arg: UInt8): UInt64 { /* compiled code */ }

    operator func -(arg: UInt8): UInt64 { /* compiled code */ }
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(Float32, Float32)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: Float32): std.core.Option<Float32> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): Float32 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): Float32 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): Float32 { /* compiled code */ }

    operator func +(arg: UInt8): Float32 { /* compiled code */ }

    operator func -(arg: UInt8): Float32 { /* compiled code */ }
}

extend `Unit` <: Torcable {
    static func randomRecentEntry(): std.core.Option<(Float64, Float64)> { /* compiled code */ }

    static func randomRecentValueComparedTo(value: Float64): std.core.Option<Float64> { /* compiled code */ }

    static func nextFrom(random: std.random.Random): Float64 { /* compiled code */ }

    static func suggestedFrom(random: std.random.Random): Float64 { /* compiled code */ }

    func isZero(): Bool { /* compiled code */ }

    func bitFlip(at: UInt8): Float64 { /* compiled code */ }

    operator func +(arg: UInt8): Float64 { /* compiled code */ }

    operator func -(arg: UInt8): Float64 { /* compiled code */ }
}

enum Mutation {
    | Drop

    | Insert(T)

    | ReplaceWith(T)

    | BitFlip(UInt8)

    | Add(UInt8)

    | Sub(UInt8)

    func apply(at: Int64, values: std.collection.ArrayList<T>): Unit { /* compiled code */ }
}

class Mutator {
    func randomRecentValue(): std.core.Option<T> { /* compiled code */ }

    func randomRecentCmp(value: T): std.core.Option<T> { /* compiled code */ }

    func randomMutation(temperature: UInt8, value: T): std.core.Option<std.unittest.Mutation<T>> { /* compiled code */ }

    let random: std.random.Random

    Mutator(random: std.random.Random) { /* compiled code */ }
}

class MutatingRandomValueStream {
    var currentIndex: Int64

    func nextUniform(random: std.random.Random): T { /* compiled code */ }

    func next(random: std.random.Random, temperature: UInt8, mutator: std.unittest.Mutator): T { /* compiled code */ }

    func reset(): std.unittest.MutatingRandomValueStream<T> { /* compiled code */ }

    func freeze(): std.unittest.MutatingRandomValueStream<T> { /* compiled code */ }

    let values: std.collection.ArrayList<T>

    MutatingRandomValueStream(values: std.collection.ArrayList<T>) { /* compiled code */ }
}

class CovRandomSource <: std.core.Object & std.unittest.prop_test.RandomSource {
    let inner: std.random.Random

    init(seed: UInt64, temperature: UInt8, mutator: std.unittest.Mutator) { /* compiled code */ }

    func copy(seed: UInt64, temperature: UInt8): std.unittest.CovRandomSource { /* compiled code */ }

    func freeze(): std.unittest.CovRandomSource { /* compiled code */ }

    override func suggestBool(): Bool { /* compiled code */ }

    override func suggestUInt8(): UInt8 { /* compiled code */ }

    override func suggestUInt16(): UInt16 { /* compiled code */ }

    override func suggestUInt32(): UInt32 { /* compiled code */ }

    override func suggestUInt64(): UInt64 { /* compiled code */ }

    override func suggestInt8(): Int8 { /* compiled code */ }

    override func suggestInt16(): Int16 { /* compiled code */ }

    override func suggestInt32(): Int32 { /* compiled code */ }

    override func suggestInt64(): Int64 { /* compiled code */ }

    override func nextFloat16(): Float16 { /* compiled code */ }

    override func nextFloat32(): Float32 { /* compiled code */ }

    override func nextFloat64(): Float64 { /* compiled code */ }

    override func suggestFloat16(): Float16 { /* compiled code */ }

    override func suggestFloat32(): Float32 { /* compiled code */ }

    override func suggestFloat64(): Float64 { /* compiled code */ }

    var gaussian: std.unittest.GaussianGenerator

    override func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64 { /* compiled code */ }

    override func nextUInt8(): UInt8 { /* compiled code */ }

    override func nextUInt16(): UInt16 { /* compiled code */ }

    override func nextUInt32(): UInt32 { /* compiled code */ }

    override func nextUInt64(): UInt64 { /* compiled code */ }

    let seed: UInt64

    let temperature: UInt8

    let valuesInt8: std.unittest.MutatingRandomValueStream<UInt8>

    let valuesInt16: std.unittest.MutatingRandomValueStream<UInt16>

    let valuesInt32: std.unittest.MutatingRandomValueStream<UInt32>

    let valuesInt64: std.unittest.MutatingRandomValueStream<UInt64>

    let valuesFloat32: std.unittest.MutatingRandomValueStream<Float32>

    let valuesFloat64: std.unittest.MutatingRandomValueStream<Float64>

    let mutator: std.unittest.Mutator
}

class GaussianGenerator {
    var cached: std.core.Option<Float64>

    func next(random: std.unittest.prop_test.RandomSource): Float64 { /* compiled code */ }

    let mean: Float64

    let sigma: Float64

    GaussianGenerator(mean: Float64, sigma: Float64) { /* compiled code */ }
}

class CoverageInfo {
    static func newCoverageDiscovered(): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

func printUnittestHelpPageIfRequested(): Bool { /* compiled code */ }

struct KeyHelp <: std.unittest.common.KeyFor<Bool> {
    static prop help: std.unittest.KeyHelp {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyNoColor <: std.unittest.common.KeyFor<Bool> {
    static prop noColor: std.unittest.KeyNoColor {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyFromTopLevel <: std.unittest.common.KeyFor<Bool> {
    static prop fromTopLevel: std.unittest.KeyFromTopLevel {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyRandomSeed <: std.unittest.common.KeyFor<Int64> {
    static prop randomSeed: std.unittest.KeyRandomSeed {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyWarmup <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Duration> {
    static prop warmup: std.unittest.KeyWarmup {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyMinDuration <: std.unittest.common.KeyFor<std.core.Duration> {
    static prop minDuration: std.unittest.KeyMinDuration {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyExplicitGC <: std.unittest.common.KeyFor<std.unittest.ExplicitGcType> {
    static prop explicitGC: std.unittest.KeyExplicitGC {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyMinBatches <: std.unittest.common.KeyFor<Int64> {
    static prop minBatches: std.unittest.KeyMinBatches {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyBatchSize <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Range<Int64>> {
    static prop batchSize: std.unittest.KeyBatchSize {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyParallel <: std.unittest.common.KeyFor<Bool> & std.unittest.common.KeyFor<std.core.String> & std.unittest.common.KeyFor<Int64> {
    static prop parallel: std.unittest.KeyParallel {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyGenerationSteps <: std.unittest.common.KeyFor<Int64> {
    static prop generationSteps: std.unittest.KeyGenerationSteps {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyReductionSteps <: std.unittest.common.KeyFor<Int64> {
    static prop reductionSteps: std.unittest.KeyReductionSteps {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeySkip <: std.unittest.common.KeyFor<Bool> {
    static prop skip: std.unittest.KeySkip {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuided <: std.unittest.common.KeyFor<Bool> {
    static prop coverageGuided: std.unittest.KeyCoverageGuided {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedInitialSeeds <: std.unittest.common.KeyFor<Int64> {
    static prop coverageGuidedInitialSeeds: std.unittest.KeyCoverageGuidedInitialSeeds {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedMaxCandidates <: std.unittest.common.KeyFor<Int64> {
    static prop coverageGuidedMaxCandidates: std.unittest.KeyCoverageGuidedMaxCandidates {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedBaselineScore <: std.unittest.common.KeyFor<Int64> {
    static prop coverageGuidedBaselineScore: std.unittest.KeyCoverageGuidedBaselineScore {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedNewCoverageScore <: std.unittest.common.KeyFor<Int64> {
    static prop coverageGuidedNewCoverageScore: std.unittest.KeyCoverageGuidedNewCoverageScore {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedNewCoverageBonus <: std.unittest.common.KeyFor<Int64> {
    static prop coverageGuidedNewCoverageBonus: std.unittest.KeyCoverageGuidedNewCoverageBonus {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyBench <: std.unittest.common.KeyFor<Bool> {
    static prop bench: std.unittest.KeyBench {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyTimeout <: std.unittest.common.KeyFor<std.core.Duration> {
    static prop timeout: std.unittest.KeyTimeout {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyTimeoutEach <: std.unittest.common.KeyFor<std.core.String> {
    static prop timeoutEach: std.unittest.KeyTimeoutEach {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyTimeoutHandler <: std.unittest.common.KeyFor<(std.unittest.TestCaseInfo) -> Unit> {
    static prop timeoutHandler: std.unittest.KeyTimeoutHandler {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    static prop captureOutput: std.unittest.KeyCaptureOutput {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyNoCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    static prop noCaptureOutput: std.unittest.KeyNoCaptureOutput {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyShowAllOutput <: std.unittest.common.KeyFor<Bool> {
    static prop showAllOutput: std.unittest.KeyShowAllOutput {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyVerbose <: std.unittest.common.KeyFor<Bool> {
    static prop verbose: std.unittest.KeyVerbose {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyMeasurement <: std.unittest.common.KeyFor<std.unittest.Measurement> {
    static prop measurement: std.unittest.KeyMeasurement {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyMeasurementInfo <: std.unittest.common.KeyFor<std.unittest.MeasurementInfo> {
    static prop measurementInfo: std.unittest.KeyMeasurementInfo {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyInternalTestrunnerInputPath <: std.unittest.common.KeyFor<std.core.String> {
    static prop internalTestrunnerInputPath: std.unittest.KeyInternalTestrunnerInputPath {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyDeathAware <: std.unittest.common.KeyFor<Bool> {
    static prop deathAware: std.unittest.KeyDeathAware {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyBaseline <: std.unittest.common.KeyFor<std.core.String> {
    static prop baseline: std.unittest.KeyBaseline {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyFilter <: std.unittest.common.KeyFor<std.core.String> {
    static prop filter: std.unittest.KeyFilter {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyIncludeTags <: std.unittest.common.KeyFor<std.core.String> {
    static prop includeTags: std.unittest.KeyIncludeTags {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyExcludeTags <: std.unittest.common.KeyFor<std.core.String> {
    static prop excludeTags: std.unittest.KeyExcludeTags {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyBaselinePath <: std.unittest.common.KeyFor<std.core.String> {
    static prop baselinePath: std.unittest.KeyBaselinePath {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyInternalWorkerId <: std.unittest.common.KeyFor<Int64> {
    static prop internalWorkerId: std.unittest.KeyInternalWorkerId {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyInternalNWorkers <: std.unittest.common.KeyFor<Int64> {
    static prop internalNWorkers: std.unittest.KeyInternalNWorkers {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyInternalWorkerSkipNTests <: std.unittest.common.KeyFor<Int64> {
    static prop internalWorkerSkipNTests: std.unittest.KeyInternalWorkerSkipNTests {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyInternalMainProcessPort <: std.unittest.common.KeyFor<UInt16> {
    static prop internalMainProcessPort: std.unittest.KeyInternalMainProcessPort {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyDryRun <: std.unittest.common.KeyFor<Bool> {
    static prop dryRun: std.unittest.KeyDryRun {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyShowTags <: std.unittest.common.KeyFor<Bool> {
    static prop showTags: std.unittest.KeyShowTags {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyNoProgress <: std.unittest.common.KeyFor<Bool> {
    static prop noProgress: std.unittest.KeyNoProgress {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyProgressBrief <: std.unittest.common.KeyFor<Bool> {
    static prop progressBrief: std.unittest.KeyProgressBrief {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    init() { /* compiled code */ }
}

struct KeyProgressEntriesLimit <: std.unittest.common.KeyFor<Int64> {
    static prop progressEntriesLimit: std.unittest.KeyProgressEntriesLimit {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
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

enum ParallelInfo {
    | NoParallel

    | Parallel(Int64)

    static func fromDefaultConfiguration(): std.unittest.ParallelInfo { /* compiled code */ }

    prop nWorkers: Int64 {
        get() { /* compiled code */ }
    }
}

class PowerAssertValue <: std.core.Object & std.unittest.common.Serializable<std.unittest.PowerAssertValue> {
    init(value: std.core.String, expression: std.core.String, position: Int64, isException: Bool) { /* compiled code */ }

    prop value: std.core.String {
        get() { /* compiled code */ }
    }

    prop range: std.core.Range<Int64> {
        get() { /* compiled code */ }
    }

    prop valuePosition: Int64 {
        get() { /* compiled code */ }
    }

    prop safeValueNextPosition: Int64 {
        get() { /* compiled code */ }
    }

    static func skipped(expression: std.core.String, position: Int64): std.unittest.PowerAssertValue { /* compiled code */ }

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValue { /* compiled code */ }

    let kind: std.unittest.PowerAssertValueKind

    let expression: std.core.String

    let position: Int64

    PowerAssertValue(kind: std.unittest.PowerAssertValueKind, expression: std.core.String, position: Int64) { /* compiled code */ }
}

enum PowerAssertValueKind <: std.unittest.common.Serializable<std.unittest.PowerAssertValueKind> {
    | PAValue(std.core.String)

    | PAThrown(std.core.String)

    | PASkipped

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValueKind { /* compiled code */ }
}

class PowerAssertDiagramBuilder {
    var wasExceptionThrown: Bool

    let expression: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    init(expression: std.core.String) { /* compiled code */ }

    func r(value: T, exprAsText: std.core.String, position: Int64): T { /* compiled code */ }

    func r(value: Rune, exprAsText: std.core.String, position: Int64): Rune { /* compiled code */ }

    func r(value: std.core.String, exprAsText: std.core.String, position: Int64): std.core.String { /* compiled code */ }

    func h(exception: std.core.Exception, exprAsText: std.core.String, position: Int64): Nothing { /* compiled code */ }

    func w(passed: Bool): Unit { /* compiled code */ }
}

foreign func EnableANSITerminal(): Bool

foreign func DisableANSITerminal(): Bool

var isConsoleUtf8: Bool

class TerminalPrettyPrinter <: std.unittest.common.PrettyPrinter {
    let areColorsEnabled: Bool

    let areOtherAnsiEnabled: Bool

    init(configuration: std.unittest.common.Configuration) { /* compiled code */ }

    prop terminalHeight: std.core.Option<UInt64> {
        get() { /* compiled code */ }
    }

    prop terminalWidth: std.core.Option<UInt64> {
        get() { /* compiled code */ }
    }

    func exclusive(action: (std.unittest.TerminalPrettyPrinter) -> T): T { /* compiled code */ }

    func up(lines: Int64): Unit { /* compiled code */ }

    func down(lines: Int64): Unit { /* compiled code */ }

    func toBottom(): Unit { /* compiled code */ }

    func setColumn(column: Int64): Unit { /* compiled code */ }

    func saveCursorPos(): Unit { /* compiled code */ }

    func restoreCursorPos(): Unit { /* compiled code */ }

    func hideCursor(): Unit { /* compiled code */ }

    func showCursor(): Unit { /* compiled code */ }

    func setScrollableMargins(start: std.core.Option<Int64>, end: std.core.Option<Int64>): Unit { /* compiled code */ }

    func clearAhead(): Unit { /* compiled code */ }

    func clearLine(): Unit { /* compiled code */ }

    override func put(s: std.core.String): Unit { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }

    override func setColor(color: std.unittest.common.Color): Unit { /* compiled code */ }

    static prop instance: std.unittest.TerminalPrettyPrinter {
        get() { /* compiled code */ }
    }

    func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.TerminalPrettyPrinter { /* compiled code */ }

    TerminalPrettyPrinter(isColored: Bool, isOtherAnsiAble: Bool) { /* compiled code */ }
}

foreign func SetConsoleOutputCP(cp: UInt32): UInt32

func setUtf8OutputCP(): Bool { /* compiled code */ }

foreign func GetTerminalWidth(): UInt64

foreign func GetTerminalHeight(): UInt64

class CapturedProcessResult {
    let stdOut: std.core.Array<UInt8>

    let stdErr: std.core.Array<UInt8>

    CapturedProcessResult(stdOut: std.core.Array<UInt8>, stdErr: std.core.Array<UInt8>) { /* compiled code */ }
}

func processStart(ctx: std.unittest.MainExecutionCtx): std.unittest.WrappedProcess { /* compiled code */ }

func renderException(exception: std.core.Exception, preamble: std.core.Option<std.unittest.common.PrettyText>): std.unittest.common.PrettyText { /* compiled code */ }

func exceptionToString(e: std.core.Exception): std.core.String { /* compiled code */ }

func indexOfFirstIrrelevantEntry(elements: std.core.Array<std.core.StackTraceElement>): Int64 { /* compiled code */ }

func isEntryPoint(_: std.core.StackTraceElement): Bool { /* compiled code */ }

func isLambda(element: std.core.StackTraceElement): Bool { /* compiled code */ }

func methodName(element: std.core.StackTraceElement): std.core.String { /* compiled code */ }

extend TestGroupResult {
    func reportCsv(dir: std.fs.Path, raw: Bool): Unit { /* compiled code */ }
}

extend TestSuiteResult {
    func reportBenchCsv(raw: Bool): std.core.ToString { /* compiled code */ }
}

let STYLES: std.core.String

let CASE_TEMPLATE: std.core.String

let ROOT_TEMPLATE: std.core.String

var gnuplotAvailable: Bool

extend TestGroupResult {
    func reportHtml(dir: std.fs.Path): Unit { /* compiled code */ }

    func collectClassObjects(passedFQNames: std.core.Iterable<std.unittest.TestCaseId>): std.collection.ArrayList<std.unittest.common.JsonObject> { /* compiled code */ }

    func envRow(name: std.core.String, value: T): std.unittest.common.JsonObject { /* compiled code */ }

    func collectEnvironment(): std.unittest.common.JsonArray { /* compiled code */ }

    func tryGetNative(f: () -> std.core.String): std.core.String { /* compiled code */ }

    func collectAllResults(directory: std.fs.Path): std.collection.HashMap<std.unittest.TestCaseId, std.unittest.common.JsonValue> { /* compiled code */ }
}

func measurementModel(step: std.unittest.BenchStepEntry, directory: std.fs.Path, index: Int64): (Float64, std.unittest.common.JsonObject) { /* compiled code */ }

struct BenchStepEntry {
    let result: std.unittest.BenchmarkResult

    let arg: std.unittest.ArgumentDescription

    let caseId: std.unittest.TestCaseId

    let measurementInfo: std.unittest.MeasurementInfo

    BenchStepEntry(result: std.unittest.BenchmarkResult, arg: std.unittest.ArgumentDescription, caseId: std.unittest.TestCaseId, measurementInfo: std.unittest.MeasurementInfo) { /* compiled code */ }
}

extend StatisticsSample {
    func createStatsRow(name: std.core.String, dir: std.fs.Path, change: std.core.Option<Float64>): std.unittest.common.JsonObject { /* compiled code */ }

    func drawKde(into: std.fs.Path): Unit { /* compiled code */ }
}

extend BenchmarkResult {
    func draw(into: std.fs.Path, mName: std.core.String): Unit { /* compiled code */ }
}

let NO_INFO: std.core.String

func getOsVersion(): std.core.String { /* compiled code */ }

func getCpuName(): std.core.String { /* compiled code */ }

func getCompilerVersion(): std.core.String { /* compiled code */ }

func getTargetTriple(): std.core.String { /* compiled code */ }

enum TestCode <: std.core.ToString & std.unittest.common.PrettyPrintable & std.core.Equatable<std.unittest.TestCode> {
    | PASS

    | SKIP

    | TIMEOUT

    | FAIL

    | ERROR

    | NORUN

    func toString(): std.core.String { /* compiled code */ }

    func mostSevereWith(other: std.unittest.TestCode): std.unittest.TestCode { /* compiled code */ }

    func isFailure(): Bool { /* compiled code */ }

    func toStringTense(): std.core.String { /* compiled code */ }

    operator func ==(right: std.unittest.TestCode): Bool { /* compiled code */ }

    operator func !=(right: std.unittest.TestCode): Bool { /* compiled code */ }

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

class XmlBuilder <: std.core.Object & std.core.ToString {
    let element_: std.unittest.XmlElementBuilder

    var version_: std.core.ToString

    var encoding_: std.core.Option<std.core.ToString>

    func version(v: std.core.ToString): std.unittest.XmlBuilder { /* compiled code */ }

    func encoding(format: std.core.ToString): std.unittest.XmlBuilder { /* compiled code */ }

    func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlBuilder { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    init() { /* compiled code */ }
}

class XmlElementBuilder <: std.core.Object & std.core.ToString {
    var name_: std.core.ToString

    var attributes_: std.collection.ArrayList<(std.core.ToString, std.core.ToString)>

    var children: std.collection.ArrayList<std.unittest.XmlElementBuilder>

    var content_: std.core.ToString

    mut prop attributes: std.collection.ArrayList<(std.core.ToString, std.core.ToString)> {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    mut prop content: std.core.ToString {
        get() { /* compiled code */ }
        set(value) { /* compiled code */ }
    }

    func name(name: std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    func attribute(key: std.core.ToString, value: std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    func optionalAttribute(key: std.core.ToString, value: std.core.Option<std.core.String>): std.unittest.XmlElementBuilder { /* compiled code */ }

    func element(contentBuilder: () -> std.core.ToString): std.unittest.XmlElementBuilder { /* compiled code */ }

    func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlElementBuilder { /* compiled code */ }

    func build(sb: std.core.StringBuilder): Unit { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    let level: UInt64

    XmlElementBuilder(level: UInt64) { /* compiled code */ }
}

extend TestSuiteId {
    func asClassName(): std.core.String { /* compiled code */ }
}

extend TestGroupResult {
    func reportXml(dir: std.fs.Path): Unit { /* compiled code */ }
}

extend TestSuiteId {
    func reportFileName(isBench: Bool, extension: std.core.String): std.core.String { /* compiled code */ }
}

class CsvReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let directory: std.fs.Path

    CsvReporter(directory: std.fs.Path) { /* compiled code */ }
}

class CsvRawReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let directory: std.fs.Path

    CsvRawReporter(directory: std.fs.Path) { /* compiled code */ }
}

class XmlReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> {
    let directory: std.fs.Path

    XmlReporter(directory: std.fs.Path) { /* compiled code */ }
}

class ConsoleReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let colored: Bool

    ConsoleReporter(colored: Bool) { /* compiled code */ }
}

class TextReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    let into: PP

    TextReporter(into: PP) { /* compiled code */ }
}

interface TextReporterBase <: std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    abstract func printReport(report: std.unittest.Report): PP
}

extend TextReporter <: TextReporterBase {
    func printReport(report: std.unittest.Report): PP { /* compiled code */ }
}

extend ConsoleReporter <: TextReporterBase {
    func printReport(report: std.unittest.Report): Unit { /* compiled code */ }
}

class RawStatsReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, std.collection.HashMap<std.core.String, (Float64, Float64)>> {
    func benchResultsAsMap(result: std.unittest.TestGroupResult): std.collection.HashMap<std.core.String, (Float64, Float64)> { /* compiled code */ }

    RawStatsReporter() { /* compiled code */ }
}

class ProgressReporter {
    let reporter: std.unittest.UTProgressReporter

    var terminalHeight: std.core.Option<UInt64>

    var terminalWidth: std.core.Option<UInt64>

    var prevReportHeight: Int64

    let printer: std.unittest.TerminalPrettyPrinter

    var reporterJob: std.core.Option<std.core.Future<Unit>>

    let isActive: std.sync.AtomicBool

    prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress> {
        get() { /* compiled code */ }
    }

    func startReporting(): Unit { /* compiled code */ }

    func stopAndClear(): Unit { /* compiled code */ }

    static func fromDefaultConfiguration(): std.core.Option<std.unittest.ProgressReporter> { /* compiled code */ }
}

interface UTProgressReporter {
    abstract prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    func start(): Unit { /* compiled code */ }

    abstract func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText

    func reset(): Unit { /* compiled code */ }
}

class TestProgressReporter <: std.core.Object & std.unittest.UTProgressReporter {
    var width: Int64

    var height: Int64

    var markWidth: Int64

    let booker: std.unittest.TestProgressCollector

    override prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress> {
        get() { /* compiled code */ }
    }

    override func start(): Unit { /* compiled code */ }

    override func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText { /* compiled code */ }

    let testCasesParallelism: Int64

    let isBrief: Bool

    let entriesLimit: std.core.Option<UInt64>

    TestProgressReporter(testCasesParallelism: Int64, isBrief: Bool, entriesLimit: std.core.Option<UInt64>) { /* compiled code */ }
}

class TestProgressCollector {
    let updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    let registeredWorkers: std.collection.HashSet<std.unittest.UniqueWorkerId>

    let totalPackageCasesCount: std.collection.HashMap<std.core.String, UInt64>

    let packagesOrder: std.collection.ArrayList<std.core.String>

    let packagesStartTime: std.collection.HashMap<std.core.String, std.time.DateTime>

    let finishedPackageCasesCount: std.collection.HashMap<std.core.String, UInt64>

    let packagesProgress: std.collection.HashMap<std.core.String, std.unittest.Progress>

    let failedPackages: std.collection.HashSet<std.core.String>

    var _totalCasesCount: UInt64

    let casesOrder: std.core.Array<std.core.Option<Int64>>

    var _finishedCasesCount: UInt64

    var _passedCasesCount: UInt64

    var _failedCasesCount: UInt64

    var _crashedWorkers: UInt64

    var _lastFailedStep: std.core.Option<std.unittest.FailedTestCaseProgressData>

    let summaryProgress: std.unittest.Progress

    var startTime: std.time.MonoTime

    let executing: std.collection.HashMap<Int64, std.unittest.TestCaseProgressData>

    let finishedPackages: std.collection.ArrayList<std.core.String>

    prop totalCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop finishedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop passedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop failedCasesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop crashedWorkersCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop registeredPackagesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop currentPackagesCount: UInt64 {
        get() { /* compiled code */ }
    }

    prop lastFailedStep: std.core.Option<std.unittest.FailedTestCaseProgressData> {
        get() { /* compiled code */ }
    }

    prop currentPackages: std.core.Array<std.unittest.PackageProgressData> {
        get() { /* compiled code */ }
    }

    prop currentTestCases: std.core.Array<std.unittest.TestCaseProgressData> {
        get() { /* compiled code */ }
    }

    prop summary: std.unittest.SummaryProgressData {
        get() { /* compiled code */ }
    }

    func start(): Unit { /* compiled code */ }

    func flushChanges(): Unit { /* compiled code */ }

    TestProgressCollector(testCasesParallelism: Int64) { /* compiled code */ }
}

struct UniqueWorkerId <: std.core.Hashable & std.core.Equatable<std.unittest.UniqueWorkerId> {
    func hashCode(): Int64 { /* compiled code */ }

    operator func ==(other: std.unittest.UniqueWorkerId): Bool { /* compiled code */ }

    operator func !=(other: std.unittest.UniqueWorkerId): Bool { /* compiled code */ }

    let id: Int64

    let pack: std.core.String

    UniqueWorkerId(id: Int64, pack: std.core.String) { /* compiled code */ }
}

class Progress <: std.core.Object & std.unittest.common.PrettyPrintable {
    var text: std.unittest.common.PrettyText

    let pieces: std.collection.ArrayList<Bool>

    var barIdx: Int64

    var drawnPieces: Int64

    var prevWidth: Int64

    var prevPieceWidth: Int64

    var prevPieceStatus: Bool

    let POSITIVE_COLOR: std.unittest.common.Color

    let NEGATIVE_COLOR: std.unittest.common.Color

    let GRAY_COLOR: std.unittest.common.Color

    func update(positive: Bool): Unit { /* compiled code */ }

    func expand(extendBy: UInt64): Unit { /* compiled code */ }

    func pprint(to: std.unittest.common.PrettyPrinter, crashedWorkers: UInt64, width: Int64, mainBar: Bool): std.unittest.common.PrettyPrinter { /* compiled code */ }

    override func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    var totalPieces: Int64

    Progress(totalPieces: Int64) { /* compiled code */ }
}

struct FailedTestCaseProgressData {
    func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    let name: std.unittest.TestCaseName

    let suiteId: std.unittest.TestSuiteId

    let check: std.unittest.RunStepResult

    FailedTestCaseProgressData(name: std.unittest.TestCaseName, suiteId: std.unittest.TestSuiteId, check: std.unittest.RunStepResult) { /* compiled code */ }
}

struct TestCaseProgressData {
    prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    static func trim(suiteId: std.unittest.TestSuiteId, name: std.unittest.TestCaseName, limit: Int64): std.core.String { /* compiled code */ }

    let name: std.unittest.TestCaseName

    let suiteId: std.unittest.TestSuiteId

    let startTime: std.time.DateTime

    TestCaseProgressData(name: std.unittest.TestCaseName, suiteId: std.unittest.TestSuiteId, startTime: std.time.DateTime) { /* compiled code */ }
}

struct PackageProgressData {
    prop finishPercent: UInt64 {
        get() { /* compiled code */ }
    }

    prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    func trimmed(limit: Int64): std.core.String { /* compiled code */ }

    let name: std.core.String

    let totalCases: UInt64

    let finishedCases: UInt64

    let isFailed: Bool

    let startTime: std.time.DateTime

    let progress: std.unittest.Progress

    PackageProgressData(name: std.core.String, totalCases: UInt64, finishedCases: UInt64, isFailed: Bool, startTime: std.time.DateTime, progress: std.unittest.Progress) { /* compiled code */ }
}

struct SummaryProgressData {
    prop finishPercent: UInt64 {
        get() { /* compiled code */ }
    }

    prop isFailed: Bool {
        get() { /* compiled code */ }
    }

    prop duration: std.core.Duration {
        get() { /* compiled code */ }
    }

    let passedCases: UInt64

    let failedCases: UInt64

    let finishedCases: UInt64

    let totalCases: UInt64

    let crashedWorkers: UInt64

    let startTime: std.time.MonoTime

    let progress: std.unittest.Progress

    SummaryProgressData(passedCases: UInt64, failedCases: UInt64, finishedCases: UInt64, totalCases: UInt64, crashedWorkers: UInt64, startTime: std.time.MonoTime, progress: std.unittest.Progress) { /* compiled code */ }
}

struct TestProgressData <: std.unittest.UTProgress {
    let data: std.unittest.ExecutionResultPart

    let workerId: Int64

    TestProgressData(data: std.unittest.ExecutionResultPart, workerId: Int64) { /* compiled code */ }
}

enum TestCaseName <: std.core.ToString {
    | CaseName(std.unittest.TestCaseId)

    | CNStep(std.unittest.LStep)

    override func toString(): std.core.String { /* compiled code */ }
}

extend ExecutionResultPart {
    func toCaseName(): std.unittest.TestCaseName { /* compiled code */ }
}

extend ExecutionResultBody {
    prop isSkip: Bool {
        get() { /* compiled code */ }
    }

    prop isFailed: Bool {
        get() { /* compiled code */ }
    }

    prop firstFailedStep: std.unittest.RunStepResult {
        get() { /* compiled code */ }
    }

    prop isPassed: Bool {
        get() { /* compiled code */ }
    }

    prop isClassLifecycle: Bool {
        get() { /* compiled code */ }
    }

    prop isBeforeAll: Bool {
        get() { /* compiled code */ }
    }
}

extend TestCaseId {
    func toDisplayString(): std.core.String { /* compiled code */ }
}

extend TestSuiteId {
    func toDisplayString(): std.core.String { /* compiled code */ }
}

struct ArgumentDescription {
    func intoErrorMessage(): std.core.Option<std.unittest.common.PrettyText> { /* compiled code */ }

    let textDescription: std.core.Option<std.unittest.common.PrettyText>

    let stepIndex: Int64

    let reductionIndex: Int64

    let randomSeed: std.core.Option<Int64>

    ArgumentDescription(textDescription: std.core.Option<std.unittest.common.PrettyText>, stepIndex: Int64, reductionIndex: Int64, randomSeed: std.core.Option<Int64>) { /* compiled code */ }
}

struct RenderOptions {
    static func fromConfiguration(configuration: std.unittest.common.Configuration): std.unittest.RenderOptions { /* compiled code */ }

    let baselineString: std.core.Option<std.core.String>

    let measurementInfo: std.core.Option<std.unittest.MeasurementInfo>

    RenderOptions(baselineString: std.core.Option<std.core.String>, measurementInfo: std.core.Option<std.unittest.MeasurementInfo>) { /* compiled code */ }
}

enum GroupResultPart <: std.unittest.Result {
    | SuiteResult(std.unittest.TestSuiteResult)

    | ProcessError(std.unittest.TestProcessError)

    func walk(visitor: (std.unittest.Result) -> Unit): Unit { /* compiled code */ }

    func visitAll(visitor: (V) -> Unit): Unit { /* compiled code */ }

    prop details: std.unittest.Details {
        get() { /* compiled code */ }
    }
}

extend LStep <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.LStep { /* compiled code */ }
}

extend StepKind <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepKind { /* compiled code */ }
}

extend StepInfo <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepInfo { /* compiled code */ }
}

extend RunStepResult {
    func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.RunStepResult { /* compiled code */ }
}

extend TestCaseResult {
    func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseResult { /* compiled code */ }
}

extend TestCaseReportInfo <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseReportInfo { /* compiled code */ }
}

extend TestSuiteResult {
    func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteResult { /* compiled code */ }
}

extend TestSuiteReportInfo <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteReportInfo { /* compiled code */ }
}

extend TestGroupResult {
    func doSerialize(): std.unittest.common.DataModelStruct { /* compiled code */ }

    static func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestGroupResult { /* compiled code */ }
}

extend RenderOptions <: Serializable {
    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.RenderOptions { /* compiled code */ }
}

struct ArrayOfTuples <: std.unittest.common.Serializable<std.unittest.ArrayOfTuples<A, B>> {
    init(col: std.core.Collection<(A, B)>) { /* compiled code */ }

    func original(): std.core.Array<(A, B)> { /* compiled code */ }

    func serializeInternal(): std.unittest.common.DataModel { /* compiled code */ }

    static func deserialize(dm: std.unittest.common.DataModel): std.unittest.ArrayOfTuples<A, B> { /* compiled code */ }

    let left: std.core.Array<A>

    let right: std.core.Array<B>

    ArrayOfTuples(left: std.core.Array<A>, right: std.core.Array<B>) { /* compiled code */ }
}

let MEDIAN_CI_95_COEFF: Float64

let MEDIAN_CI_99_COEFF: Float64

let MEDIAN_CI_999_COEFF: Float64

let EPSILON: Float64

class Measurements {
    var slopes: std.unittest.Sample

    var mainSlope: Float64

    var intercept: Float64

    var errorEst: Float64

    var r2: Float64

    func calculateMedian(): std.unittest.Measurements { /* compiled code */ }

    func calcSlopes(): Unit { /* compiled code */ }

    func calculateMean(): std.unittest.Measurements { /* compiled code */ }

    let data: std.core.Array<(Float64, Float64)>

    Measurements(data: std.core.Array<(Float64, Float64)>) { /* compiled code */ }
}

interface AsFloat {
    abstract func asFloat(): Float64
}

extend `Unit` <: AsFloat {
    func asFloat(): Float64 { /* compiled code */ }
}

class FlatMapProcessor <: std.unittest.DataStrategyProcessor<R> {
    var provider: std.unittest.RememberingDataProvider<R>

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    func lastItem(_: std.unittest.common.Configuration): R { /* compiled code */ }

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T) -> std.unittest.common.DataProvider<R>

    FlatMapProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataProvider<R>) { /* compiled code */ }
}

class LazyCyclicNode {
    var next_: std.unittest.LazyCyclicNode

    var prev: std.unittest.LazyCyclicNode

    func initialize(): std.unittest.LazyCyclicNode { /* compiled code */ }

    func advance(): std.core.Option<Unit> { /* compiled code */ }

    func recover(): Unit { /* compiled code */ }

    func add(other: std.unittest.LazyCyclicNode): Unit { /* compiled code */ }

    func removeSelf(): std.unittest.LazyCyclicNode { /* compiled code */ }

    init() { /* compiled code */ }
}

class FlatMapStrategyProcessor <: std.unittest.DataStrategyProcessor<R> {
    var provider: std.unittest.RememberingDataProvider<R>

    var strategy: std.unittest.common.DataStrategy<R>

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    func lastItem(_: std.unittest.common.Configuration): R { /* compiled code */ }

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T) -> std.unittest.common.DataStrategy<R>

    FlatMapStrategyProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataStrategy<R>) { /* compiled code */ }
}

class MapProcessor <: std.unittest.DataStrategyProcessor<R> {
    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R> { /* compiled code */ }

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R> { /* compiled code */ }

    func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    func lastItem(configuration: std.unittest.common.Configuration): R { /* compiled code */ }

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T, std.unittest.common.Configuration) -> R

    MapProcessor(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T, std.unittest.common.Configuration) -> R) { /* compiled code */ }
}

class CartesianProductProcessor <: std.unittest.DataStrategyProcessor<(T0, T1)> {
    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<(T0, T1)> { /* compiled code */ }

    func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    func lastItem(configuration: std.unittest.common.Configuration): (T0, T1) { /* compiled code */ }

    let left: std.unittest.DataStrategyProcessor<T0>

    let right: std.unittest.DataStrategyProcessor<T1>

    CartesianProductProcessor(left: std.unittest.DataStrategyProcessor<T0>, right: std.unittest.DataStrategyProcessor<T1>) { /* compiled code */ }
}

class CartesianProductIterator <: std.core.Iterator<(A, B)> {
    var leftElement: std.core.Option<A>

    var right: std.core.Iterator<B>

    override func next(): std.core.Option<(A, B)> { /* compiled code */ }

    let left: std.core.Iterator<A>

    let rightGen: () -> std.core.Iterator<B>

    CartesianProductIterator(left: std.core.Iterator<A>, rightGen: () -> std.core.Iterator<B>) { /* compiled code */ }
}

class SimpleProcessor <: std.unittest.DataStrategyProcessor<T> {
    var dataProvider: std.unittest.RememberingDataProvider<T>

    var _delegate: std.core.Option<std.unittest.common.DataStrategy<T>>

    prop strategy: std.unittest.common.DataStrategy<T> {
        get() { /* compiled code */ }
    }

    prop isInfinite: Bool {
        get() { /* compiled code */ }
    }

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T> { /* compiled code */ }

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T> { /* compiled code */ }

    func lastItemInfo(): std.core.Array<std.unittest.InputParameter> { /* compiled code */ }

    func lastItem(_: std.unittest.common.Configuration): T { /* compiled code */ }

    let buildDelegate: () -> std.unittest.common.DataStrategy<T>

    let name: std.core.String

    SimpleProcessor(buildDelegate: () -> std.unittest.common.DataStrategy<T>, name: std.core.String) { /* compiled code */ }
}

class RememberingIterable <: std.core.Iterator<T> {
    func next(): std.core.Option<T> { /* compiled code */ }

    let delegate: std.core.Iterator<T>

    let source: std.unittest.RememberingDataProvider<T>

    RememberingIterable(delegate: std.core.Iterator<T>, source: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

class RememberingLazyIterator <: std.core.Iterator<T> {
    func next(): std.core.Option<T> { /* compiled code */ }

    let delegate: std.unittest.RememberingLazyIterable<T>

    RememberingLazyIterator(delegate: std.unittest.RememberingLazyIterable<T>) { /* compiled code */ }
}

class RememberingLazyIterable <: std.unittest.LazyCyclicNode & std.core.Iterable<T> {
    var currentItem: std.core.Option<T>

    var origLastItem: T

    func recover(): Unit { /* compiled code */ }

    func advance(): std.core.Option<Unit> { /* compiled code */ }

    func iterator(): std.core.Iterator<T> { /* compiled code */ }

    let delegate: std.core.Iterator<T>

    let storage: std.unittest.RememberingDataProvider<T>

    RememberingLazyIterable(delegate: std.core.Iterator<T>, storage: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

class IterableMix <: std.core.Iterator<T> {
    func next(): std.core.Option<T> { /* compiled code */ }

    let delegate1: std.core.Iterator<T>

    let delegate2: std.core.Iterator<T>

    IterableMix(delegate1: std.core.Iterator<T>, delegate2: std.core.Iterator<T>) { /* compiled code */ }
}

class LazyCyclicNodeEngine <: std.core.Iterator<T> {
    func next(): std.core.Option<T> { /* compiled code */ }

    func advance(): std.core.Option<Unit> { /* compiled code */ }

    var current: std.unittest.LazyCyclicNode

    let iter: std.core.Iterator<T>

    LazyCyclicNodeEngine(current: std.unittest.LazyCyclicNode, iter: std.core.Iterator<T>) { /* compiled code */ }
}

type ShrinkResult = std.unittest.RunStepResult

enum ProcessorResult {
    | Success(std.unittest.RunStepResult)

    | Failure(std.core.Array<std.unittest.InputParameter>, std.unittest.RunStepResult)

    | NoItems

    | StrategyError(std.core.Exception)
}

extend DataStrategyProcessor {
    func map(f: (T) -> R): std.unittest.MapProcessor<T, R> { /* compiled code */ }

    func mapWithConfig(f: (T, std.unittest.common.Configuration) -> R): std.unittest.MapProcessor<T, R> { /* compiled code */ }

    func flatMap(f: (T) -> std.unittest.common.DataProvider<R>): std.unittest.FlatMapProcessor<T, R> { /* compiled code */ }

    func flatMapStrategy(f: (T) -> std.unittest.common.DataStrategy<R>): std.unittest.FlatMapStrategyProcessor<T, R> { /* compiled code */ }

    func product(p: std.unittest.DataStrategyProcessor<R>): std.unittest.CartesianProductProcessor<T, R> { /* compiled code */ }

    func productWithUnit(p: P): std.unittest.MapProcessor<(T, Unit), T> { /* compiled code */ }
}

class TestSuiteExecutor {
    let suiteConfiguration: std.unittest.common.Configuration

    let suiteInfo: std.unittest.TestSuiteReportInfo

    func execute(): std.unittest.TestSuiteResult { /* compiled code */ }

    func runSkip(testCaseResult: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    func runNoRun(testCaseResult: std.unittest.TestCaseResult): Unit { /* compiled code */ }

    func runSingleCase(caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor): (std.unittest.TestCaseResult) -> Unit { /* compiled code */ }

    let externalConfiguration: std.unittest.common.Configuration

    let suiteId: std.unittest.TestSuiteId

    let testSuite: std.unittest.TestSuite

    let filterService: std.unittest.FilterService

    TestSuiteExecutor(externalConfiguration: std.unittest.common.Configuration, suiteId: std.unittest.TestSuiteId, testSuite: std.unittest.TestSuite, filterService: std.unittest.FilterService) { /* compiled code */ }
}

extend TestSuite {
    prop wholeConfiguration: std.unittest.common.Configuration {
        get() { /* compiled code */ }
    }

    prop allCasesIterator: std.core.Iterable<std.unittest.CaseOrBench> {
        get() { /* compiled code */ }
    }

    func allCasesToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): std.core.Array<std.unittest.CaseOrBench> { /* compiled code */ }

    func casesCountToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): Int64 { /* compiled code */ }

    func runBeforeEach(caseShortName: std.core.String): Unit { /* compiled code */ }

    func runAfterEach(caseShortName: std.core.String): Unit { /* compiled code */ }

    func runBeforeAll(): Unit { /* compiled code */ }

    func runAfterAll(): Unit { /* compiled code */ }
}

class TempDirectory {
    let lock: std.sync.ReentrantMutex

    let rnd: std.random.Random

    let buffer: std.core.Array<Rune>

    var currentOffset: Int64

    var tempDir: std.core.Option<std.fs.Path>

    prop path: std.fs.Path {
        get() { /* compiled code */ }
    }

    func createTempFile(dir: std.fs.Path, prefix: std.core.String, suffix: std.core.String): std.fs.File { /* compiled code */ }

    func createTempPath(dir: std.fs.Path, prefix: std.core.String, suffix: std.core.String): std.fs.Path { /* compiled code */ }

    init() { /* compiled code */ }
}

func substituteTemplate(template: std.core.String, values: std.unittest.common.JsonValue): std.core.String { /* compiled code */ }

class Root {
    var templates: std.collection.HashMap<std.core.String, std.unittest.Template>

    var rootTemplate: std.unittest.Template

    func substitute(values: std.unittest.common.JsonValue): std.core.String { /* compiled code */ }

    Root(t: std.core.String) { /* compiled code */ }
}

class Template {
    static func fromString(text: std.core.String): std.unittest.Template { /* compiled code */ }

    var REPEATABLES_MATCHER: std.regex.Regex

    var VARIABLES_MATCHER: std.regex.Regex

    let REPETITIONS_SANITY_CHECK: Int64

    func substitute(values: std.unittest.common.JsonValue, templates: std.collection.HashMap<std.core.String, std.unittest.Template>, index: Int64): std.core.Option<std.core.String> { /* compiled code */ }

    let name: std.core.String

    let baseText: std.core.String

    Template(name: std.core.String, baseText: std.core.String) { /* compiled code */ }
}

func object(data: std.core.Array<(std.core.String, std.unittest.IntoJson)>): std.unittest.common.JsonObject { /* compiled code */ }

interface IntoJson {
    abstract func json(): std.unittest.common.JsonValue
}

extend JsonValue <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend String <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend `Unit` <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend Option <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend Array <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

extend ArrayList <: IntoJson {
    func json(): std.unittest.common.JsonValue { /* compiled code */ }
}

func runTestStep(caseId: std.unittest.TestCaseId, f: () -> Unit): Unit { /* compiled code */ }

class TestCaseExecutor <: std.core.Object & std.unittest.Executor {
    var generationStep: Int64

    var randomSeed: std.core.Option<Int64>

    var hasMoreParameters: Bool

    func execute(_: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

    static func formatArguments(inputs: std.core.Array<std.unittest.InputParameter>): std.core.Option<std.unittest.common.PrettyText> { /* compiled code */ }

    let strategy: std.unittest.DataStrategyProcessor<T>

    let body: (T) -> Unit

    TestCaseExecutor(strategy: std.unittest.DataStrategyProcessor<T>, body: (T) -> Unit) { /* compiled code */ }
}

extend RunStepResult {
    func withUpdatedStep(newKind: std.unittest.StepKind): std.unittest.RunStepResult { /* compiled code */ }
}

func strategyErrorAsResult(e: std.core.Exception, descr: std.unittest.ArgumentDescription): std.unittest.RunStepResult { /* compiled code */ }

let SHOW_ALL_OUTPUT_KEY: std.core.String

let NO_CAPTURE_OUTPUT_KEY: std.core.String

let OP_CAPTURE_OUTPUT: std.core.String

let CAPTURE_OUTPUT_KEY: std.core.String

let OP_VERBOSE: std.core.String

let VERBOSE_KEY: std.core.String

func tagsToString(tags: std.core.Array<std.core.String>): std.core.Option<std.core.String> { /* compiled code */ }

extend Configuration {
    prop bench: Bool {
        get() { /* compiled code */ }
    }
}

class PackageTextReport {
    var baselineReport: std.collection.HashMap<std.core.String, Float64>

    var baselineId: std.core.String

    var failures: std.collection.HashMap<std.unittest.TestCaseId, Int64>

    func printPackage(packageResult: std.unittest.TestGroupResult): Unit { /* compiled code */ }

    let pp: std.unittest.common.PrettyPrinter

    let outputReporter: std.unittest.TestOutputReporter

    let config: std.unittest.common.Configuration

    let detailed: Bool

    PackageTextReport(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration, detailed: Bool) { /* compiled code */ }
}

func appendDashLine(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

class TextReportEntry {
    let checkResults: std.core.Array<std.unittest.CheckResult>

    let duration: std.core.Duration

    let statusCode: std.unittest.TestCode

    let preamble: std.unittest.common.PrettyText

    let caseInfo: std.unittest.TestCaseReportInfo

    let bodyMessage: std.core.Option<std.unittest.common.PrettyText>

    TextReportEntry(source: std.unittest.RunStepResult, caseName: std.core.String, caseInfo: std.unittest.TestCaseReportInfo, bodyMessage: std.core.Option<std.unittest.common.PrettyText>) { /* compiled code */ }
}

class ProjectTextReport {
    func printProject(projectResult: std.unittest.ProjectExecutionResult): Unit { /* compiled code */ }

    let pp: std.unittest.common.PrettyPrinter

    let outputReporter: std.unittest.TestOutputReporter

    let config: std.unittest.common.Configuration

    ProjectTextReport(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration) { /* compiled code */ }
}

class TextReports {
    static func printDefaultReport(pp: std.unittest.common.PrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    static func printProjectSummaryReport(projectResult: std.unittest.ProjectExecutionResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    static func printIntermediatePackageResult(pp: std.unittest.TerminalPrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

func lifecycleStageFailedText(ls: std.unittest.LStep, newLine: Bool): std.unittest.common.PrettyText { /* compiled code */ }

func printSummary(pp: std.unittest.common.PrettyPrinter, result: std.unittest.ResultContainer<T>, config: std.unittest.common.Configuration, printFailures: Bool): Unit { /* compiled code */ }

extend CheckResult <: PrettyPrintable {
    func pprintWithFailedPrefix(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }
}

let OP_TIMEOUT_EACH_NAME: std.core.String

let OP_TIMEOUT_EACH: std.core.String

let TIMEOUT_EACH_KEY: std.core.String

let VALUE_TIMEOUT_PATTERN: std.regex.Regex

func cancellationPoint(): Unit { /* compiled code */ }

func fillTimeoutKey(configuration: std.unittest.common.Configuration): Unit { /* compiled code */ }

class TransportMain <: std.core.Object & std.core.Resource {
    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }

    prop port: UInt16 {
        get() { /* compiled code */ }
    }

    func accept(): std.unittest.TransportChannel { /* compiled code */ }

    static func fromDefaultConfiguration(): std.unittest.TransportMain { /* compiled code */ }

    let socket: std.net.TcpServerSocket

    let outputReporter: std.unittest.TestOutputReporter
}

struct WorkerOutput {
    prop stdouts: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    prop stderrs: std.core.Array<std.core.String> {
        get() { /* compiled code */ }
    }

    prop captured: std.core.Array<std.unittest.CapturedOutput> {
        get() { /* compiled code */ }
    }

    let stdout: std.core.Array<UInt8>

    let stderr: std.core.Array<UInt8>

    WorkerOutput(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }
}

class UnittestException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class ErrorWrapperException <: std.core.Exception {
    init(error: std.core.Error) { /* compiled code */ }

    init(message: std.core.String, error: std.core.Error) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

class UnittestTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

foreign func CJ_FS_IsFileORLinkToFile(path: CString): Int8

foreign func CJ_FS_IsDirORLinkToFDir(path: CString): Int8

extend DataModel {
    func asStruct(): std.unittest.common.DataModelStruct { /* compiled code */ }
}

func min(a: T, b: T): T { /* compiled code */ }

func max(a: T, b: T): T { /* compiled code */ }

func mapParallelOrdered(bufferSize: Int64, f: (T) -> V): (std.core.Iterable<T>) -> std.core.Iterator<V> { /* compiled code */ }

class ParallelOrderedMapIterator <: std.core.Iterator<T> {
    var futures: std.collection.ArrayList<std.core.Future<T>>

    var currentIdx: Int64

    func next(): std.core.Option<T> { /* compiled code */ }

    let bufferSize: Int64

    let iter: std.core.Iterator<std.core.Future<T>>

    ParallelOrderedMapIterator(bufferSize: Int64, iter: std.core.Iterator<std.core.Future<T>>) { /* compiled code */ }
}

class ChainIterator <: std.core.Iterator<T> {
    override func next(): std.core.Option<T> { /* compiled code */ }

    let l: std.core.Iterator<T>

    let r: std.core.Iterator<T>

    ChainIterator(l: std.core.Iterator<T>, r: std.core.Iterator<T>) { /* compiled code */ }
}

func chain(l: std.core.Iterable<T>, r: std.core.Iterable<T>): std.core.Iterable<T> { /* compiled code */ }

func beforeEach(action: (std.core.Option<T>) -> Unit): (std.core.Iterable<T>) -> std.core.Iterator<T> { /* compiled code */ }
