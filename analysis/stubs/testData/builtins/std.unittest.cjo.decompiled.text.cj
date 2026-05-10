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
    func `get`(_: Int64): T

    func createOrExisting(arg: T, x: Int64): std.unittest.ImmutableInputProvider<T>

    func createOrExisting(arg: U): U

    let data: T

    init(data: T) { /* compiled code */ }
}

interface BenchInputProvider <: std.unittest.BenchmarkInputMarker {
    func reset(max: Int64): Unit

    func `get`(idx: Int64): T
}

struct BatchInputProvider <: std.unittest.BenchInputProvider<T> {
    var data: std.core.Array<T>

    func reset(max: Int64): Unit

    func `get`(idx: Int64): T

    let builder: () -> T

    init(builder: () -> T) { /* compiled code */ }
}

struct BatchSizeOneInputProvider <: std.unittest.BenchInputProvider<T> {
    var data: T

    func reset(max: Int64): Unit

    func `get`(_: Int64): T

    let builder: () -> T

    init(builder: () -> T) { /* compiled code */ }
}

struct GenerateEachInputProvider <: std.unittest.BenchInputProvider<T> {
    func reset(_: Int64): Unit

    func `get`(_: Int64): T

    let builder: () -> T

    init(builder: () -> T) { /* compiled code */ }
}

class TestSuiteResult <: std.unittest.ResultContainer<std.unittest.TestCaseResult> {
    func merge(other: std.unittest.TestSuiteResult): Unit

    func mergeAll(suiteResults: std.core.Iterable<std.unittest.TestSuiteResult>): std.core.Collection<std.unittest.TestSuiteResult>

    let suiteId: std.unittest.TestSuiteId

    let suiteInfo: std.unittest.TestSuiteReportInfo

    init(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo) { /* compiled code */ }
}

interface BenchTable {
    func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable>

    func doBuild: std.unittest.PrettyTable

    prop hasBaselineHeader: Bool
}

class BenchTableImpl <: std.core.Object & std.unittest.BenchTable {
    let subgroups: std.collection.ArrayList<std.unittest.BenchTableImpl>

    prop hasBaselineHeader: Bool

    func putResultToBaselineReport(key: std.core.String, value: Float64): Unit

    func getResultFromBaselineReport(key: std.core.String): std.core.Option<Float64>

    func build(suiteResult: std.unittest.TestSuiteResult, baselineReport: std.collection.HashMap<std.core.String, Float64>): std.core.Option<std.unittest.BenchTable>

    func isEmpty: Bool

    func initTableStructure: Unit

    func doBuild: std.unittest.PrettyTable

    func doTableBuild: Unit

    func addSubgroup(subgroup: std.unittest.BenchTableImpl): Unit

    let props: std.unittest.BenchTableProperties

    let table: std.unittest.PrettyTable

    init(props: std.unittest.BenchTableProperties, table: std.unittest.PrettyTable) { /* compiled code */ }
}

func GetRdtscp: UInt64

interface TestFilter {
    func shouldSkipTestClass(suiteFilterKey: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(caseFilterKey: std.unittest.CaseFilterKey): Bool
}

class CompositeTestFilter <: std.core.Object & std.unittest.TestFilter {
    func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool

    let filters: std.core.Array<std.unittest.TestFilter>

    init(filters: std.core.Array<std.unittest.TestFilter>) { /* compiled code */ }
}

class TagsTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    let includeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    let excludeTagFilters: std.collection.ArrayList<std.unittest.TagsFilterOption>

    init(includeArgs: std.core.Array<std.core.String>, excludeArgs: std.core.Array<std.core.String>) { /* compiled code */ }

    func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TagsTestFilter

    func fromDefaultConfiguration: std.unittest.TagsTestFilter

    func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool
}

class FilterOptionTestFilter <: std.core.Object & std.unittest.UserTestFilter {
    let includeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    let excludeFilters: std.collection.ArrayList<std.unittest.FilterOption>

    init() { /* compiled code */ }

    init(filterArgs: std.core.Array<std.core.String>) { /* compiled code */ }

    func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.FilterOptionTestFilter

    func fromDefaultConfiguration: std.unittest.FilterOptionTestFilter

    func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool
}

interface UserTestFilter <: std.unittest.TestFilter {
    func fromConfiguration(config: std.unittest.common.Configuration): std.unittest.TestFilter

    func fromDefaultConfiguration: std.unittest.TestFilter
}

func defaultConfiguration: std.unittest.common.Configuration

struct SuiteFilterKey {
    func fromTestSuite(groupName: std.core.String, suite: std.unittest.TestSuite): std.unittest.SuiteFilterKey

    let suiteId: std.unittest.TestSuiteId

    let tags: std.core.Array<std.core.String>

    let topLevelFunc: std.core.Option<std.core.String>

    init(suiteId: std.unittest.TestSuiteId, tags: std.core.Array<std.core.String>, topLevelFunc: std.core.Option<std.core.String>) { /* compiled code */ }
}

class TestSuite {
    prop name: std.core.String

    func runTests: std.unittest.TestReport

    func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport

    func runBenchmarks: std.unittest.BenchReport

    func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport

    func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult

    func execute(groupName: std.core.String, runConfiguration: std.unittest.common.Configuration, filterService: std.unittest.FilterService): std.unittest.TestSuiteResult

    func builder(name: std.core.String): std.unittest.TestSuiteBuilder

    func builder(suite: std.unittest.TestSuite): std.unittest.TestSuiteBuilder

    let _name: std.core.String

    let cases: std.core.Array<std.unittest.CaseOrBench>

    let suiteConfiguration: std.unittest.common.Configuration

    let beforeAlls: std.core.Array<() -> Unit>

    let beforeEachs: std.core.Array<(std.core.String) -> Unit>

    let afterEachs: std.core.Array<(std.core.String) -> Unit>

    let afterAlls: std.core.Array<() -> Unit>

    let template: std.core.Option<std.unittest.TestSuite>

    init(_name: std.core.String, cases: std.core.Array<std.unittest.CaseOrBench>, suiteConfiguration: std.unittest.common.Configuration, beforeAlls: std.core.Array<() -> Unit>, beforeEachs: std.core.Array<(std.core.String) -> Unit>, afterEachs: std.core.Array<(std.core.String) -> Unit>, afterAlls: std.core.Array<() -> Unit>, template: std.core.Option<std.unittest.TestSuite>) { /* compiled code */ }
}

interface ReportFormat {
    prop isBench: Bool

    func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat

    func default(path: std.fs.Path): std.unittest.ReportFormat

    func fromDefaultConfiguration: std.core.Option<std.unittest.ReportFormat>

    func report(testInfo: std.unittest.TestGroupResult): Unit
}

enum BenchReportFormat <: std.unittest.ReportFormat {
    | CSV(std.fs.Path)

    | CSV_RAW(std.fs.Path)

    | Html(std.fs.Path)

    prop isBench: Bool

    func default(path: std.fs.Path): std.unittest.ReportFormat

    func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat

    func report(result: std.unittest.TestGroupResult): Unit
}

enum UTestReportFormat <: std.unittest.ReportFormat {
    | XML(std.fs.Path)

    func default(path: std.fs.Path): std.unittest.ReportFormat

    prop isBench: Bool

    func fromString(format: std.core.String, path: std.fs.Path): std.unittest.ReportFormat

    func report(result: std.unittest.TestGroupResult): Unit
}

struct KeyReportPath <: std.unittest.common.KeyFor<std.core.String> {
    prop reportPath: std.unittest.KeyReportPath

    prop name: std.core.String

    init() { /* compiled code */ }
}

func getReportDirectory(key: Key): std.core.Option<std.fs.Path>

struct KeyReportFormat <: std.unittest.common.KeyFor<std.core.String> {
    prop reportFormat: std.unittest.KeyReportFormat

    prop name: std.core.String

    init() { /* compiled code */ }
}

class UnittestCliOptionsFormatException <: std.unittest.UnittestException {
    init(optionName: std.core.String, actual: std.core.String, expected: std.core.String) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

interface Result <: std.unittest.common.Serializable<std.unittest.Result> {
    func visitChildren(_: (std.unittest.Result) -> Unit): Unit

    func walk(visitor: (std.unittest.Result) -> Unit): Unit

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.Result

    prop details: std.unittest.Details
}

class RunStepResult <: std.core.Object & std.unittest.Result {
    prop details: std.unittest.Details

    func simpleError(step: std.unittest.StepKind, check: std.unittest.CheckResult): std.unittest.RunStepResult

    func hasFailures: Bool

    prop checkResults: std.core.Array<std.unittest.CheckResult>

    func statusCode: std.unittest.TestCode

    let checksPassed: Int64

    let startTimestamp: std.time.DateTime

    let kind: std.unittest.StepKind

    let info: std.unittest.StepInfo

    let duration: std.core.Duration

    init(checksPassed: Int64, startTimestamp: std.time.DateTime, kind: std.unittest.StepKind, info: std.unittest.StepInfo, duration: std.core.Duration) { /* compiled code */ }
}

class TestCaseResult <: std.unittest.ResultContainer<std.unittest.RunStepResult> {
    func fromSingleStep(id: std.unittest.TestCaseId, info: std.unittest.TestCaseReportInfo, step: std.unittest.RunStepResult): std.unittest.TestCaseResult

    var _output: std.unittest.CapturedOutput

    prop output: std.unittest.CapturedOutput

    func addOutput(output: std.unittest.CapturedOutput): Unit

    func addSuccessfullStep(next: std.unittest.RunStepResult): Unit

    func hasFailures: Bool

    let caseId: std.unittest.TestCaseId

    let caseInfo: std.unittest.TestCaseReportInfo

    let renderOptions: std.unittest.RenderOptions

    init(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, renderOptions: std.unittest.RenderOptions) { /* compiled code */ }
}

class TestGroupResult <: std.unittest.ResultContainer<std.unittest.GroupResultPart> {
    func fromPackageSuites(packageName: std.core.String, suiteResults: std.core.Collection<std.unittest.TestSuiteResult>, processErrors: std.core.Array<std.unittest.TestProcessError>, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>): std.unittest.TestGroupResult

    prop hasProcessError: Bool

    func add(testSuite: std.unittest.Result): Unit

    let groupName: std.core.String

    let nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>

    init(groupName: std.core.String, nonTestOutputs: std.core.Array<std.unittest.NonTestOutputs>) { /* compiled code */ }
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

    func add(other: std.unittest.Details): Unit

    prop totalCount: Int64

    prop isSuccess: Bool

    var testcode: std.unittest.TestCode

    var isAccountedStep: Bool

    init(testcode: std.unittest.TestCode, isAccountedStep: Bool) { /* compiled code */ }
}

class RememberingDataProvider <: std.core.Object & std.unittest.common.DataProvider<T> {
    var info: std.unittest.InputParameter

    var current: std.core.Option<T>

    func provide: std.core.Iterable<T>

    let delegate: std.unittest.common.DataProvider<T>

    init(name: std.core.String, delegate: std.unittest.common.DataProvider<T>) { /* compiled code */ }
}

struct MeasurementInfo {
    let conversionTable: std.core.Array<(Float64, std.core.String)>

    let name: std.core.String

    let textDescription: std.core.String

    init(conversionTable: std.core.Array<(Float64, std.core.String)>, name: std.core.String, textDescription: std.core.String) { /* compiled code */ }
}

class TestReport <: std.unittest.Report {
    func reportTo(reporter: std.unittest.Reporter<std.unittest.TestReport, T>): T

    init(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

class BenchReport <: std.unittest.Report {
    func reportTo(reporter: std.unittest.Reporter<std.unittest.BenchReport, T>): T

    init(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

enum ExecutionMode {
    | OnlyTests

    | OnlyBenchmarks

    func asFilter: std.unittest.FrameworkTestFilter
}

struct FilterService {
    func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit

    func fromDefaultConfiguration: std.unittest.FilterService

    let userFilter: std.unittest.TestFilter

    let frameworkFilter: std.unittest.FrameworkTestFilter

    init(userFilter: std.unittest.TestFilter, frameworkFilter: std.unittest.FrameworkTestFilter) { /* compiled code */ }
}

class TestSuiteBuilder {
    let beforeAlls: std.collection.ArrayList<() -> Unit>

    let beforeEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    let afterEachs: std.collection.ArrayList<(std.core.String) -> Unit>

    let afterAlls: std.collection.ArrayList<() -> Unit>

    var configuration: std.unittest.common.Configuration

    var tests: std.collection.ArrayList<std.unittest.CaseOrBench>

    var template_: std.core.Option<std.unittest.TestSuite>

    func setName(name: std.core.String): std.unittest.TestSuiteBuilder

    func configure(configuration: std.unittest.common.Configuration): std.unittest.TestSuiteBuilder

    func beforeEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder

    func afterEach(body: (std.core.String) -> Unit): std.unittest.TestSuiteBuilder

    func beforeEach(body: () -> Unit): std.unittest.TestSuiteBuilder

    func afterEach(body: () -> Unit): std.unittest.TestSuiteBuilder

    func beforeAll(body: () -> Unit): std.unittest.TestSuiteBuilder

    func afterAll(body: () -> Unit): std.unittest.TestSuiteBuilder

    func add(test: std.unittest.UnitTestCase): std.unittest.TestSuiteBuilder

    func add(benchmark: std.unittest.Benchmark): std.unittest.TestSuiteBuilder

    func template(template: std.unittest.TestSuite): std.unittest.TestSuiteBuilder

    func build: std.unittest.TestSuite

    var name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

enum CaseOrBench {
    | Case(std.unittest.UnitTestCase)

    | Bench(std.unittest.Benchmark)

    prop caseConfiguration: std.unittest.common.Configuration

    prop name: std.core.String

    prop isBench: Bool

    prop executor: std.unittest.Executor
}

class TestGroup {
    prop name: std.core.String

    func runTests: std.unittest.TestReport

    func runTests(configuration: std.unittest.common.Configuration): std.unittest.TestReport

    func runBenchmarks: std.unittest.BenchReport

    func runBenchmarks(configuration: std.unittest.common.Configuration): std.unittest.BenchReport

    func run(configuration: std.unittest.common.Configuration, mode: std.unittest.ExecutionMode): std.unittest.TestGroupResult

    func builder(name: std.core.String): std.unittest.TestGroupBuilder

    func builder(group: std.unittest.TestGroup): std.unittest.TestGroupBuilder

    let _name: std.core.String

    let suites: std.core.Array<std.unittest.TestSuite>

    let groupConfiguration: std.unittest.common.Configuration

    init(_name: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, groupConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

class TestGroupBuilder {
    let suites: std.collection.ArrayList<std.unittest.TestSuite>

    var configuration: std.unittest.common.Configuration

    func setName(name: std.core.String): std.unittest.TestGroupBuilder

    func configure(configuration: std.unittest.common.Configuration): std.unittest.TestGroupBuilder

    func add(test: std.unittest.UnitTestCase): std.unittest.TestGroupBuilder

    func add(benchmark: std.unittest.Benchmark): std.unittest.TestGroupBuilder

    func add(suite: std.unittest.TestSuite): std.unittest.TestGroupBuilder

    func build: std.unittest.TestGroup

    var name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

class UnitTestCase {
    prop name: std.core.String

    func run: std.unittest.TestReport

    func createParameterized(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration: std.unittest.common.Configuration, body: (T) -> Unit): std.unittest.UnitTestCase

    func createParameterized(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, body: (T) -> Unit): std.unittest.UnitTestCase

    func create(name: std.core.String, configuration: std.unittest.common.Configuration, body: () -> Unit): std.unittest.UnitTestCase

    let _name: std.core.String

    let caseConfiguration: std.unittest.common.Configuration

    let executor: std.unittest.Executor

    init(_name: std.core.String, caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

class Benchmark {
    prop name: std.core.String

    func run: std.unittest.BenchReport

    func createWithExplicitLoop(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, optLambda: (T, Int64, Int64) -> Float64): std.unittest.Benchmark

    func createParameterized(name: std.core.String, strategy: std.unittest.common.DataStrategy<T>, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: (T) -> Unit): std.unittest.Benchmark

    func createParameterized(name: std.core.String, strategy: std.unittest.DataStrategyProcessor<T>, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: (T) -> Unit): std.unittest.Benchmark

    func create(name: std.core.String, configuration: std.unittest.common.Configuration, measurement: std.unittest.Measurement, body: () -> Unit): std.unittest.Benchmark

    let _name: std.core.String

    let benchmarkConfiguration: std.unittest.common.Configuration

    let executor: std.unittest.Executor

    init(_name: std.core.String, benchmarkConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor) { /* compiled code */ }
}

class DataStrategyProcessor {
    var genIterator: std.core.Iterator<T>

    var currentReduction: Int64

    prop isInfinite: Bool

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T>

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(configuration: std.unittest.common.Configuration): T

    func nextIteration(configuration: std.unittest.common.Configuration): Unit

    func processNextWith(_: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.unittest.ProcessorResult

    func shrink(configuration: std.unittest.common.Configuration, runSingle: (T, std.core.Array<std.unittest.InputParameter>) -> std.unittest.RunStepResult): std.core.Option<std.unittest.ProcessorResult>

    func intoBenchmark(caseName: std.core.String, configuration: std.unittest.common.Configuration, doRun: (T, Int64, Int64) -> Float64): std.unittest.Benchmark

    func intoUnitTestCase(caseName: std.core.String, configuration: std.unittest.common.Configuration, doRun: (T) -> Unit): std.unittest.UnitTestCase

    func start(s: std.unittest.common.DataStrategy<T>, name: std.core.String): std.unittest.SimpleProcessor<T>

    func start(f: () -> std.unittest.common.DataStrategy<U>, name: std.core.String): std.unittest.DataStrategyProcessor<U>

    func start(f: () -> std.unittest.common.DataStrategy<T>, name: std.core.String, x: Int64): std.unittest.SimpleProcessor<T>

    func start(f: () -> std.unittest.DataStrategyProcessor<T>, _: std.core.String): std.unittest.DataStrategyProcessor<T>

    func start(f: () -> std.unittest.DataStrategyProcessor<U>, _: std.core.String, x: Int64): std.unittest.DataStrategyProcessor<U>

    init() { /* compiled code */ }
}

interface Executor {
    func execute(suiteInfo: std.unittest.TestSuiteReportInfo, caseResult: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit
}

interface Measurement {
    func setup: Unit

    func measure: Float64

    prop conversionTable: std.core.Array<(Float64, std.core.String)>

    prop name: std.core.String

    prop textDescription: std.core.String

    prop info: std.unittest.MeasurementInfo
}

struct TestCaseInfo {
    let groupName: std.core.String

    let suiteName: std.core.String

    let caseName: std.core.String

    init(groupName: std.core.String, suiteName: std.core.String, caseName: std.core.String) { /* compiled code */ }
}

class Report {
    let details: std.unittest.Details

    prop errorCount: Int64

    prop caseCount: Int64

    prop passedCount: Int64

    prop failedCount: Int64

    prop skippedCount: Int64

    let groupResult: std.unittest.TestGroupResult

    let runConfiguration: std.unittest.common.Configuration

    init(groupResult: std.unittest.TestGroupResult, runConfiguration: std.unittest.common.Configuration) { /* compiled code */ }
}

interface Reporter {
}

interface TestClass {
    func asTestSuite: std.unittest.TestSuite
}

class TestPackage {
    let builder: std.unittest.TestGroupBuilder

    func registerCase(testCase: () -> std.unittest.UnitTestCase): Unit

    func registerSuite(suite: () -> std.unittest.TestSuite): Unit

    func registerBench(bench: () -> std.unittest.Benchmark): Unit

    func build: std.unittest.TestGroup

    let name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

interface FrameworkTestFilter <: std.unittest.TestFilter {
    func register(_: std.unittest.SuiteFilterKey, _: std.unittest.TestSuite): Unit
}

extend TestGroupResult {
    func asBenchReport(runConfiguration: std.unittest.common.Configuration): std.unittest.BenchReport

    func asTestReport(runConfiguration: std.unittest.common.Configuration): std.unittest.TestReport
}

func assertEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

class AssertionCtx {
    let _stringArgs: std.core.String

    var _argsMap: std.core.Option<std.collection.HashMap<std.core.String, std.core.String>>

    let _errors: std.collection.ArrayList<std.unittest.PrettyTreeStruct>

    prop caller: std.core.String

    prop hasErrors: Bool

    func arg(key: std.core.String): std.core.String

    prop args: std.core.String

    func setArgsAliases(aliases: std.core.Array<std.core.String>): Unit

    func fail(message: std.core.String): Nothing

    func fail(pt: PP): Nothing

    func failExpect(message: std.core.String): Unit

    func failExpect(pt: PP): Unit

    let _passedArgs: std.core.Array<std.core.String>

    let _caller: std.core.String

    init(_passedArgs: std.core.Array<std.core.String>, _caller: std.core.String) { /* compiled code */ }
}

func assertEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta: Bool, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

func expectEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

func expectEqual(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, isDelta: Bool, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

func buildCheckResult(leftStr: std.core.String, rightStr: std.core.String, expected: T, actual: T, checkResultKind: std.unittest.CheckResultKind, isDelta: Bool): std.unittest.AssertExpectCheckResult

enum CheckResultKind <: std.unittest.common.Serializable<std.unittest.CheckResultKind> {
    | Assert

    | Expect

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResultKind
}

class AssertExpectCheckResult <: std.unittest.CheckResult {
    let kind: std.unittest.CheckResultKind

    init(kind: std.unittest.CheckResultKind) { /* compiled code */ }
}

class AssertException <: std.core.Exception {
    let cause: std.core.Option<std.unittest.CheckResult>

    init() { /* compiled code */ }

    init(message: std.core.String, cause: std.unittest.CheckResult) { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class CheckResult <: std.core.Object & std.unittest.common.Serializable<std.unittest.CheckResult> {
    func deserialize(dm: std.unittest.common.DataModel): std.unittest.CheckResult

    init() { /* compiled code */ }
}

class AssertIntermediateException <: std.core.Exception {
    let diagram: std.unittest.PowerAssertDiagram

    let expression: std.core.String

    let originalException: std.core.Exception

    init(exception: std.core.Exception, expression: std.core.String, message: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }

    func getOriginalStackTrace: std.core.String

    func getClassName: std.core.String
}

class PowerAssertDiagram <: std.core.Object & std.core.ToString & std.unittest.common.Serializable<std.unittest.PowerAssertDiagram> & std.unittest.common.PrettyPrintable {
    let diagram: std.collection.ArrayList<std.collection.ArrayList<std.unittest.PowerAssertValue>>

    var offset: Int64

    init() { /* compiled code */ }

    func setOffset(offset: Int64): Unit

    func addValue(newValue: std.unittest.PowerAssertValue): Unit

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

    func toString: std.core.String

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertDiagram

    func pprintExpression(expr: std.core.String, pp: std.unittest.common.PrettyPrinter): std.core.Any
}

interface BenchmarkInputMarker {
}

struct SimpleBenchWrapper <: std.unittest.BenchmarkWrapper {
    func measureLoopOnce(times: Int64, max: Int64): Float64

    func timeLoopOnce(times: Int64, max: Int64): Float64

    let input: T

    let f: (T, Int64, Int64) -> Float64

    init(input: T, f: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

interface BenchmarkWrapper {
    func measureLoopOnce(times: Int64, max: Int64): Float64

    func timeLoopOnce(times: Int64, max: Int64): Float64
}

interface BenchmarkConfig {
    func batchSize(b: Int64): Unit

    func batchSize(x: std.core.Range<Int64>): Unit

    func warmup(x: Int64): Unit

    func warmup(x: std.core.Duration): Unit

    func minDuration(x: std.core.Duration): Unit

    func explicitGC(x: std.unittest.ExplicitGcType): Unit

    func minBatches(x: Int64): Unit
}

enum ExplicitGcType <: std.core.ToString {
    | Disabled

    | Light

    | Heavy

    func invoke: Unit

    func toString: std.core.String
}

extend Configuration <: BenchmarkConfig {
    func batchSize(b: Int64): Unit

    func batchSize(x: std.core.Range<Int64>): Unit

    func warmup(x: Int64): Unit

    func warmup(x: std.core.Duration): Unit

    func minDuration(x: std.core.Duration): Unit

    func explicitGC(x: std.unittest.ExplicitGcType): Unit

    func minBatches(x: Int64): Unit
}

type BenchRawMeasurement = (Float64, Float64)

class BenchExecutor <: std.core.Object & std.unittest.Executor {
    var step: Int64

    func execute(suiteInfo: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit

    func runSingle(value: T, inputs: std.core.Array<std.unittest.InputParameter>, suiteInfo: std.unittest.TestSuiteReportInfo, caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, benchRunner: std.unittest.BenchRunner, progress: std.unittest.ProgressOutput, configuration: std.unittest.common.Configuration, step: Int64): std.unittest.RunStepResult

    let strategy: std.unittest.DataStrategyProcessor<T>

    let doRun: (T, Int64, Int64) -> Float64

    init(strategy: std.unittest.DataStrategyProcessor<T>, doRun: (T, Int64, Int64) -> Float64) { /* compiled code */ }
}

struct TestSuiteReportInfo {
    prop isEmpty: Bool

    let tags: std.core.Array<std.core.String>

    init(tags: std.core.Array<std.core.String>) { /* compiled code */ }
}

class InputParameter {
    var position: Int64

    var repr: std.core.String

    func update(param: T): Unit

    let name: std.core.String

    init(name: std.core.String) { /* compiled code */ }
}

struct TestCaseId <: std.unittest.common.Serializable<std.unittest.TestCaseId> & std.core.Hashable & std.core.Equatable<std.unittest.TestCaseId> & std.core.ToString {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseId

    operator func ==(other: std.unittest.TestCaseId): Bool

    operator func !=(other: std.unittest.TestCaseId): Bool

    func hashCode: Int64

    func fqName: std.core.String

    func toString: std.core.String

    let suiteId: std.unittest.TestSuiteId

    let caseName: std.core.String

    let isBench: Bool

    init(suiteId: std.unittest.TestSuiteId, caseName: std.core.String, isBench: Bool) { /* compiled code */ }
}

struct TestCaseReportInfo {
    func empty: std.unittest.TestCaseReportInfo

    prop isEmpty: Bool

    func merge(suiteInfo: std.unittest.TestSuiteReportInfo): std.unittest.TestCaseReportInfo

    let tags: std.core.Array<std.core.String>

    init(tags: std.core.Array<std.core.String>) { /* compiled code */ }
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

    func runBench: Unit

    func runBenchInner: Unit

    func calcBatchSize(estimation: Float64): (std.core.Range<Int64>, Int64)

    let configuration: std.unittest.common.Configuration

    let progress: std.unittest.ProgressOutput

    init(configuration: std.unittest.common.Configuration, progress: std.unittest.ProgressOutput) { /* compiled code */ }
}

class ProgressOutput {
    let isVerbose: Bool

    func println(message: () -> std.core.String): Unit

    func verboseLine(message: std.core.String): Unit

    func verbose(message: std.core.String): Unit

    init(configuration: std.unittest.common.Configuration) { /* compiled code */ }
}

struct StatisticsSample {
    let _kde: std.collection.ArrayList<(Float64, Float64)>

    init() { /* compiled code */ }

    func append(data: Float64): Unit

    func finish: Unit

    prop mean: Float64

    prop median: Float64

    prop lowerBound: Float64

    prop upperBound: Float64

    prop kde: std.collection.ArrayList<(Float64, Float64)>

    prop mode: Float64

    func mode2: Float64

    let sample: std.unittest.Sample

    init(sample: std.unittest.Sample) { /* compiled code */ }
}

class Sample {
    prop size: Int64

    func mean: Float64

    func percentile(ratio: Float64): Float64

    func create(data: std.collection.ArrayList<Float64>): std.unittest.Sample

    func kde(kdeThroughput: Float64): std.collection.ArrayList<(Float64, Float64)>

    let data: std.collection.ArrayList<Float64>

    init(data: std.collection.ArrayList<Float64>) { /* compiled code */ }
}

let BOOTSTRAP_ITERATIONS: Int64

class BenchmarkResult {
    var finished: Bool

    var bootstrapped: std.unittest.BootstrapResult

    prop median: Float64

    prop medianCI99: Float64

    prop medianOverhead: Float64

    prop mean: Float64

    prop meanCI99: Float64

    prop meanLower: Float64

    prop meanUpper: Float64

    prop meanOverhead: Float64

    prop mainResult: Float64

    prop errorEst: Float64

    func calculate: Unit

    func valuesSample: std.unittest.StatisticsSample

    var data: std.collection.ArrayList<(Float64, Float64)>

    init(data: std.collection.ArrayList<(Float64, Float64)>) { /* compiled code */ }
}

struct BootstrapResult {
    let meanStats: std.unittest.StatisticsSample

    let meanOverheadStats: std.unittest.StatisticsSample

    let medianStats: std.unittest.StatisticsSample

    let medianOverheadStats: std.unittest.StatisticsSample

    let r2Stats: std.unittest.StatisticsSample

    let buffer: std.collection.ArrayList<Float64>

    func join(other: std.unittest.BootstrapResult): std.unittest.BootstrapResult

    func finish: Unit

    func appendStatisticsFromSample(subsample: std.collection.ArrayList<(Float64, Float64)>): Unit

    init() { /* compiled code */ }
}

func percentToColor(p: Float64): std.unittest.common.Color

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

    init(hasArgsColumn: Bool, hasCasesBaseline: Bool, hasMeasurementColumn: Bool, hasRatioColumn: Bool, shouldPrintMeasurement: Bool, conversionTable: std.core.Array<(Float64, std.core.String)>, measurementName: std.core.String, baseline: std.unittest.Baseline, baselineReport: std.collection.HashMap<std.core.String, Float64>) { /* compiled code */ }
}

class PrettyTable <: std.core.Object & std.unittest.common.PrettyPrintable {
    let columnSizes: std.collection.ArrayList<Int64>

    let rows: std.collection.ArrayList<std.collection.ArrayList<(std.core.String, std.unittest.common.Color)>>

    var column: Int64

    let leftAlignedColumns: std.collection.HashSet<Int64>

    init() { /* compiled code */ }

    func addColumns(names: T): Unit

    func addColumn(name: std.core.String): Unit

    func removeLastColumn: (std.core.String, std.unittest.common.Color)

    func alignColumnLeft(idx: Int64): Bool

    func addEmptyRow: Unit

    func nextRow: Unit

    func removeCurrentRow: std.collection.ArrayList<(std.core.String, std.unittest.common.Color)>

    func addCell(value: std.core.String, color: std.unittest.common.Color): Unit

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

    let names: std.collection.ArrayList<std.core.String>

    init(names: std.collection.ArrayList<std.core.String>) { /* compiled code */ }
}

struct TimeNow <: std.unittest.Measurement {
    var unit: std.core.Option<std.unittest.TimeUnit>

    init(unit: std.core.Option<std.unittest.TimeUnit>) { /* compiled code */ }

    init() { /* compiled code */ }

    func measure: Float64

    prop conversionTable: std.core.Array<(Float64, std.core.String)>

    prop name: std.core.String

    prop textDescription: std.core.String
}

struct Ema {
    var result: Float64

    var currentLength: Float64

    func reset: Unit

    func reachedMax: Bool

    func append(next: Float64): Unit

    let maxLength: Int64

    init(maxLength: Int64) { /* compiled code */ }
}

type MeasurementUnitTable = std.core.Array<(Float64, std.core.String)>

let RADIX_UNIT_TABLE: std.core.Array<(Float64, std.core.String)>

func printDuration(dur: std.core.Duration): std.core.String

enum TimeUnit <: std.core.ToString {
    | Nanos

    | Micros

    | Millis

    | Seconds

    func toString: std.core.String

    func inNanos: Float64

    func conversionRow: (Float64, std.core.String)
}

func GetRdtsc: UInt64

struct CpuCycles <: std.unittest.Measurement {
    func measure: Float64

    func setup: Unit

    prop conversionTable: std.core.Array<(Float64, std.core.String)>

    prop name: std.core.String

    prop textDescription: std.core.String

    init() { /* compiled code */ }
}

class UnattachedCheckResult <: std.unittest.CheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.UnattachedCheckResult>

    let inner: std.unittest.CheckResult

    init(inner: std.unittest.CheckResult) { /* compiled code */ }
}

class ErrorCheckResult <: std.unittest.CheckResult {
    init() { /* compiled code */ }
}

class PlainCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainCompareCheckResult>

    let leftExpr: std.core.String

    let rightExpr: std.core.String

    let leftValue: std.core.String

    let rightValue: std.core.String

    init(kind: std.unittest.CheckResultKind, leftExpr: std.core.String, rightExpr: std.core.String, leftValue: std.core.String, rightValue: std.core.String) { /* compiled code */ }
}

class PlainDeltaCompareCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PlainDeltaCompareCheckResult>

    let expr: std.core.String

    init(kind: std.unittest.CheckResultKind, expr: std.core.String) { /* compiled code */ }
}

class FailCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.FailCheckResult>

    let failMessage: std.core.String

    init(kind: std.unittest.CheckResultKind, failMessage: std.core.String) { /* compiled code */ }
}

class AssertThrowsCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.AssertThrowsCheckResult>

    let message: std.core.String

    let expectedExceptions: std.core.String

    let caughtException: std.core.String

    init(kind: std.unittest.CheckResultKind, message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String) { /* compiled code */ }
}

class PowerAssertCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertCheckResult>

    let expr: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    init(expr: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

class PowerAssertExceptionCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.PowerAssertExceptionCheckResult>

    let expression: std.core.String

    let exception: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    init(expression: std.core.String, exception: std.core.String, diagram: std.unittest.PowerAssertDiagram) { /* compiled code */ }
}

class DiffCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult>

    let diffMessage: std.unittest.common.PrettyText

    init(kind: std.unittest.CheckResultKind, diffMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

class CustomCheckResult <: std.unittest.AssertExpectCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.DiffCheckResult>

    let custom: std.unittest.common.PrettyText

    init(kind: std.unittest.CheckResultKind, custom: std.unittest.common.PrettyText) { /* compiled code */ }
}

class ExceptionThrownCheckResult <: std.unittest.ErrorCheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.ExceptionThrownCheckResult>

    let errorMessage: std.unittest.common.PrettyText

    let stackTrace: std.unittest.common.PrettyText

    init(errorMessage: std.unittest.common.PrettyText, stackTrace: std.unittest.common.PrettyText) { /* compiled code */ }
}

class MockFrameworkCheckResult <: std.unittest.CheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.MockFrameworkCheckResult>

    let mockFrameworkMessage: std.unittest.common.PrettyText

    init(mockFrameworkMessage: std.unittest.common.PrettyText) { /* compiled code */ }
}

class NoParametersProvidedResult <: std.unittest.CheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.NoParametersProvidedResult>

    init() { /* compiled code */ }
}

class CrashedCheckResult <: std.unittest.CheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.CrashedCheckResult>

    let exitCode: Int64

    init(exitCode: Int64) { /* compiled code */ }
}

class TimeoutCheckResult <: std.unittest.CheckResult {
    func serializeInternal: std.unittest.common.DataModel

    func tryToDeserialize(dms: std.unittest.common.DataModelStruct): std.core.Option<std.unittest.TimeoutCheckResult>

    let msg: std.unittest.common.PrettyText

    init(msg: std.unittest.common.PrettyText) { /* compiled code */ }
}

enum PrettyTreeStruct <: std.unittest.common.PrettyPrintable {
    | Leaf(std.unittest.AssertExpectCheckResult)

    | Node(std.core.String, std.collection.ArrayList<std.unittest.PrettyTreeStruct>)

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

func invokeCustomAssert(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, assert: (std.unittest.AssertionCtx) -> T, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): T

func invokeCustomExpect(passedArgs: std.core.Array<std.core.String>, caller: std.core.String, expect: (std.unittest.AssertionCtx) -> std.core.Any, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

func fillAbsentValues(config: std.unittest.common.Configuration): Unit

func kebabCaseToCamelCase(str: std.core.String): std.core.String

func camelCaseToKebabCase(str: std.core.String): std.core.String

func setValueWithGuessedType(configuration: std.unittest.common.Configuration, key: std.core.String, value: std.core.String): Unit

func buildDefaultConfiguration: std.unittest.common.Configuration

var defaultConfigurationCached: std.sync.AtomicOptionReference<std.unittest.common.Configuration>

func setValuesFromJsonConfiguration(configuration: std.unittest.common.Configuration, rawJson: std.core.String): Unit

func typeValidator(name: std.core.String): (std.core.Any) -> std.unittest.common.OptionValidity

func registerUnittestOption(key: std.unittest.common.KeyFor<T>, description: std.core.Option<std.core.String>, typeDescription: std.core.Option<(std.core.String, std.core.String)>): Unit

interface NearEquatable {
    func isNear(obj: CT, delta: D): Bool
}

struct RelativeDelta {
    let absolute: T

    let relative: T

    init(absolute: T, relative: T) { /* compiled code */ }
}

func isNearExpansion(l: CT, r: CT, delta: D, cmpType: std.core.String): Bool

func isNearExpansion(l: CT, r: CT, delta: D, cmpType: std.core.String, overloadHack: Bool): Bool

extend Extend <: NearEquatable & NearEquatable {
    func isNear(obj: Float16, delta: Float16): Bool

    func isNear(obj: Float16, delta: std.unittest.RelativeDelta<Float16>): Bool
}

extend Extend <: NearEquatable & NearEquatable {
    func isNear(obj: Float32, delta: Float32): Bool

    func isNear(obj: Float32, delta: std.unittest.RelativeDelta<Float32>): Bool
}

extend Extend <: NearEquatable & NearEquatable {
    func isNear(obj: Float64, delta: Float64): Bool

    func isNear(obj: Float64, delta: std.unittest.RelativeDelta<Float64>): Bool
}

func entryMain(testPackage: std.unittest.TestPackage): Int64

func testRunnerEntryMain: Int64

class ModuleExecutionResult <: std.unittest.ResultContainer<std.unittest.TestGroupResult> {
    let moduleName: std.core.String

    init(moduleName: std.core.String) { /* compiled code */ }
}

class ResultContainer <: std.core.Object & std.unittest.Result {
    let subResults: std.collection.ArrayList<T>

    func visitChildren(visitor: (std.unittest.Result) -> Unit): Unit

    func add(child: T): Unit

    var _details: std.unittest.Details

    var _detailsComputed: Bool

    var finished: Bool

    prop isFinished: Bool

    prop details: std.unittest.Details

    func finish: Unit

    func visitAll(visitor: (V) -> Unit): Unit

    func visitPassedTests(visitor: (std.unittest.TestCaseResult) -> Unit): Unit

    func visitPassedBenches(visitor: (std.unittest.BenchStepEntry) -> Unit): Unit

    func visitFailedSteps(visitor: (std.unittest.RunStepResult, std.unittest.TestCaseResult) -> Unit): Unit

    func serializeSubResults: std.unittest.common.DataModelStruct

    init() { /* compiled code */ }
}

class ProjectExecutionResult <: std.unittest.ResultContainer<std.unittest.ModuleExecutionResult> {
    init() { /* compiled code */ }
}

class PackageExecutionResult {
    let packageResult: std.unittest.TestGroupResult

    init(packageResult: std.unittest.TestGroupResult) { /* compiled code */ }
}

struct TestPackageExecuteCommand <: std.unittest.common.Serializable<std.unittest.TestPackageExecuteCommand> {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestPackageExecuteCommand

    func overridingCurrentEnv: std.unittest.TestPackageExecuteCommand

    func withJsonConfiguration: std.unittest.TestPackageExecuteCommand

    func withRunnerOption: std.unittest.TestPackageExecuteCommand

    func withArgs(newArgs: std.core.Array<std.core.String>): std.unittest.TestPackageExecuteCommand

    func fromCurrentProcess: std.unittest.TestPackageExecuteCommand

    let command: std.core.String

    let args: std.core.Array<std.core.String>

    let env: std.collection.Map<std.core.String, std.core.String>

    init(command: std.core.String, args: std.core.Array<std.core.String>, env: std.collection.Map<std.core.String, std.core.String>) { /* compiled code */ }
}

struct ParallelCtx {
    let nWorkersPerPackage: Int64

    let workersQuota: std.sync.Semaphore

    init(nWorkersPerPackage: Int64, workersQuota: std.sync.Semaphore) { /* compiled code */ }
}

struct ReportCtx {
    let perPackageReporter: std.unittest.PerPackageReporter

    let outputReporter: std.unittest.TestOutputReporter

    init(perPackageReporter: std.unittest.PerPackageReporter, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

class PerPackageReporter {
    func printIntermediateResult(executionResult: std.unittest.PackageExecutionResult): Unit

    func dumpPackageReport(executionResult: std.unittest.PackageExecutionResult): std.core.Option<Unit>

    let format: std.core.Option<std.unittest.ReportFormat>

    let outputReporter: std.unittest.TestOutputReporter

    init(format: std.core.Option<std.unittest.ReportFormat>, outputReporter: std.unittest.TestOutputReporter) { /* compiled code */ }
}

class TestOutputReporter {
    func report(pp: std.unittest.common.PrettyPrinter, prefix: std.unittest.common.PrettyText, output: std.unittest.CapturedOutput, success: Bool): std.unittest.common.PrettyPrinter

    func fromConfiguration(conf: std.unittest.common.Configuration): std.unittest.TestOutputReporter

    func fromDefaultConfiguration: std.unittest.TestOutputReporter

    let capture: Bool

    let showAllOutput: Bool

    let verbose: Bool
}

let EXIT_CODE_ON_TIMEOUT: Int64

struct MainExecutionCtx {
    func withCommand(command: std.unittest.TestPackageExecuteCommand): std.unittest.MainExecutionCtx

    let nWorkers: Int64

    let workersQuota: std.sync.Semaphore

    let outputReporter: std.unittest.TestOutputReporter

    let executeCommand: std.unittest.TestPackageExecuteCommand

    init(nWorkers: Int64, workersQuota: std.sync.Semaphore, outputReporter: std.unittest.TestOutputReporter, executeCommand: std.unittest.TestPackageExecuteCommand) { /* compiled code */ }
}

func executeSmart(packageName: std.core.String, tests: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService, outputReporter: std.unittest.TestOutputReporter, needStartWorker: Bool): std.unittest.TestGroupResult

func initWorkersMain(ctx: std.unittest.MainExecutionCtx): std.core.Array<std.unittest.WorkerProcess>

class WorkerProcess {
    func waitCasesRegistration: std.core.Option<Bool>

    func commitCasesRegistrationFailure: std.core.Option<Bool>

    func create(ctx: std.unittest.MainExecutionCtx, workerId: Int64, progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>, connect: () -> std.unittest.TransportChannel): std.unittest.WorkerProcess

    func createAndRegister(workerId: Int64, ctx: std.unittest.MainExecutionCtx): std.unittest.WorkerProcess

    func executeRestarting(initialWorker: std.unittest.WorkerProcess, ctx: std.unittest.MainExecutionCtx): std.unittest.ExecuteRestartingResult

    let process: std.unittest.WrappedProcess

    let transportChannel: std.core.Option<std.unittest.TransportChannel>

    let progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    let outputReporter: std.unittest.TestOutputReporter

    let workerId: Int64
}

func executeMain(workers: std.core.Array<std.unittest.WorkerProcess>, packageName: std.core.String, ctx: std.unittest.MainExecutionCtx): std.unittest.TestGroupResult

func uniqueValues(collection: std.core.Collection<T>): std.core.Array<T>

func executeWorker(packageName: std.core.String, suites: std.core.Array<std.unittest.TestSuite>, filterService: std.unittest.FilterService): std.unittest.TestGroupResult

enum TestProcessKind {
    | Main

    | Worker(Int64, Int64, Int64, UInt16)

    func fromDefaultConfiguration: std.unittest.TestProcessKind

    prop isWorker: Bool
}

struct NonTestOutputs <: std.core.Hashable {
    func hashCode: Int64

    init() { /* compiled code */ }

    init(pair: (std.unittest.CapturedOutput, std.unittest.CapturedOutput)) { /* compiled code */ }

    func isEmpty: Bool

    let outputBeforeTests: std.unittest.CapturedOutput

    let outputAfterTests: std.unittest.CapturedOutput

    init(outputBeforeTests: std.unittest.CapturedOutput, outputAfterTests: std.unittest.CapturedOutput) { /* compiled code */ }
}

struct CapturedOutput {
    init() { /* compiled code */ }

    init(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }

    func isEmpty: Bool

    let stdout: std.core.String

    let stderr: std.core.String

    init(stdout: std.core.String, stderr: std.core.String) { /* compiled code */ }
}

enum ExecuteRestartingResult {
    | OkResult(std.core.Collection<std.unittest.TestSuiteResult>, std.collection.ArrayList<std.unittest.NonTestOutputs>)

    | CrashResult(std.core.Collection<std.unittest.TestSuiteResult>, std.unittest.TestProcessError, std.collection.ArrayList<std.unittest.NonTestOutputs>)
}

class TestProcessError <: std.core.Object & std.unittest.Result & std.core.Hashable {
    let _details: std.unittest.Details

    func hashCode: Int64

    prop details: std.unittest.Details

    let code: Int64

    init(code: Int64) { /* compiled code */ }
}

interface UTProgress {
}

class TransportChannel <: std.core.Object & std.core.Resource {
    prop port: UInt16

    func isClosed: Bool

    func close: Unit

    func send(part: std.unittest.ExecutionResultPart): Unit

    func receive(limit: std.core.Option<UInt64>): std.core.Iterator<std.unittest.ExecutionResultPart>

    func delimitOutput: Unit

    let socket: std.net.TcpSocket

    let outputReporter: std.unittest.TestOutputReporter

    let isMain: Bool

    init(socket: std.net.TcpSocket, outputReporter: std.unittest.TestOutputReporter, isMain: Bool) { /* compiled code */ }
}

struct WrappedProcess {
    let status: std.sync.AtomicOptionReference<std.unittest.CapturedProcessResult>

    func wait: Int64

    func terminate: Unit

    func stdout: std.core.Array<UInt8>

    func stderr: std.core.Array<UInt8>

    let process: std.process.SubProcess

    let stdOutFile: std.core.Option<std.fs.Path>

    let stdErrFile: std.core.Option<std.fs.Path>

    init(process: std.process.SubProcess, stdOutFile: std.core.Option<std.fs.Path>, stdErrFile: std.core.Option<std.fs.Path>) { /* compiled code */ }
}

class ExecutionResultPart <: std.core.Object & std.unittest.common.Serializable<std.unittest.ExecutionResultPart> {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.ExecutionResultPart

    init() { /* compiled code */ }
}

class ExecutionResultHeader <: std.unittest.ExecutionResultPart {
    prop suiteId: std.unittest.TestSuiteId

    prop suiteInfo: std.unittest.TestSuiteReportInfo

    prop startTime: std.time.DateTime

    init() { /* compiled code */ }
}

struct TestSuiteId <: std.unittest.common.Serializable<std.unittest.TestSuiteId> & std.core.Hashable & std.core.Equatable<std.unittest.TestSuiteId> & std.core.ToString {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteId

    operator func ==(other: std.unittest.TestSuiteId): Bool

    operator func !=(other: std.unittest.TestSuiteId): Bool

    func hashCode: Int64

    func toString: std.core.String

    func fromTestSuite(groupName: std.core.String, testSuite: std.unittest.TestSuite): std.unittest.TestSuiteId

    prop isFromTopLevelFunc: Bool

    let groupName: std.core.String

    let suiteName: std.core.String

    init(groupName: std.core.String, suiteName: std.core.String) { /* compiled code */ }
}

class TestCaseExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    prop suiteId: std.unittest.TestSuiteId

    prop suiteInfo: std.unittest.TestSuiteReportInfo

    prop startTime: std.time.DateTime

    let caseId: std.unittest.TestCaseId

    let caseInfo: std.unittest.TestCaseReportInfo

    let suiteInfo_: std.unittest.TestSuiteReportInfo

    let startTime_: std.time.DateTime

    init(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo_: std.unittest.TestSuiteReportInfo, startTime_: std.time.DateTime) { /* compiled code */ }
}

class LifecycleExecutionResultHeader <: std.unittest.ExecutionResultHeader {
    prop suiteId: std.unittest.TestSuiteId

    prop suiteInfo: std.unittest.TestSuiteReportInfo

    prop startTime: std.time.DateTime

    let suiteId_: std.unittest.TestSuiteId

    let suiteInfo_: std.unittest.TestSuiteReportInfo

    let step: std.unittest.LStep

    let startTime_: std.time.DateTime

    init(suiteId_: std.unittest.TestSuiteId, suiteInfo_: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep, startTime_: std.time.DateTime) { /* compiled code */ }
}

enum LStep <: std.core.ToString {
    | BeforeEach

    | BeforeAll

    | AfterAll

    | AfterEach

    operator func ==(other: std.unittest.LStep): Bool

    func toString: std.core.String
}

class ExecutionResultBody <: std.unittest.ExecutionResultPart {
    let stepResult: std.unittest.TestCaseResult

    init(stepResult: std.unittest.TestCaseResult) { /* compiled code */ }
}

class TestCasesRegistrationPart <: std.unittest.ExecutionResultPart {
    let casesCount: UInt64

    let workerId: Int64

    let packageName: std.core.String

    init(casesCount: UInt64, workerId: Int64, packageName: std.core.String) { /* compiled code */ }
}

class TestCasesRegistrationFailed <: std.unittest.ExecutionResultPart {
    init() { /* compiled code */ }
}

class ExecutionPermitPart <: std.unittest.ExecutionResultPart {
    init() { /* compiled code */ }
}

func failExpect(message: std.core.String): Unit

func fail(message: std.core.String): Nothing

func assertCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Nothing

func expectCaughtUnexpectedE(message: std.core.String, expectedExceptions: std.core.String, caughtException: std.core.String, optParentCtx: std.core.Option<std.unittest.AssertionCtx>): Unit

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
    func fromTestCase(suiteId: std.unittest.TestSuiteId, cob: std.unittest.CaseOrBench, config: std.core.Option<std.unittest.common.Configuration>): std.unittest.CaseFilterKey

    let caseId: std.unittest.TestCaseId

    let tags: std.core.Array<std.core.String>

    let isTopLevel: Bool

    init(caseId: std.unittest.TestCaseId, tags: std.core.Array<std.core.String>, isTopLevel: Bool) { /* compiled code */ }
}

class BenchTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    func shouldSkipTestClass(_: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool

    func fromDefaultConfiguration: std.unittest.BenchTestFilter

    let benchEnabled: Bool

    init(benchEnabled: Bool) { /* compiled code */ }
}

struct FilterOption {
    let ACCEPTING: std.unittest.FilterOption

    func fromString(str: std.core.String): std.unittest.FilterOption

    prop classFilter: std.core.String

    prop funcFilter: std.core.String

    let _classRegex: std.core.String

    let _funcRegex: std.core.String

    let exclude: Bool

    let isFuncFilter: Bool

    init(_classRegex: std.core.String, _funcRegex: std.core.String, exclude: Bool, isFuncFilter: Bool) { /* compiled code */ }
}

class TagsFilterOption {
    func fromString(input: std.core.String): std.unittest.TagsFilterOption

    let tags: std.collection.HashSet<std.core.String>

    init(tags: std.collection.HashSet<std.core.String>) { /* compiled code */ }
}

class WorkerTestFilter <: std.core.Object & std.unittest.FrameworkTestFilter {
    let shouldSkipClassCache: std.collection.HashMap<std.unittest.TestSuiteId, Bool>

    let shouldSkipCaseCache: std.collection.HashMap<std.unittest.TestCaseId, Bool>

    var testEntityIdx: Int64

    var testCaseIdx: Int64

    func register(filterKey: std.unittest.SuiteFilterKey, suite: std.unittest.TestSuite): Unit

    func shouldSkipTestClass(filterKey: std.unittest.SuiteFilterKey): Bool

    func shouldSkipTestCase(filterKey: std.unittest.CaseFilterKey): Bool

    let defaultFilter: std.unittest.FrameworkTestFilter

    let workerId: Int64

    let nWorkers: Int64

    let nCasesSkip: Int64

    init(defaultFilter: std.unittest.FrameworkTestFilter, workerId: Int64, nWorkers: Int64, nCasesSkip: Int64) { /* compiled code */ }
}

enum LaunchApi {
    | FromPublicApi

    | FromCli
}

enum LaunchSource {
    | TestBinary

    | TestRunner

    func fromDefaultConfiguration: std.unittest.LaunchSource
}

class Framework {
    var transport: std.core.Option<std.unittest.Transport>

    var progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>

    let mtx: std.sync.ReentrantMutex

    var currentlyRunning: std.unittest.RunContext

    func launch(api: std.unittest.LaunchApi, hasWorkersInSetup: Bool, progressQueue: std.core.Option<std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>>, body: () -> T): T

    func isExecuting: Bool

    func runStepBody(stepKind: std.unittest.StepKind, info: std.unittest.StepInfo, body: () -> Unit): std.unittest.RunStepResult

    func runCatching(f: () -> Unit): Unit

    func withCurrentContext(f: (std.unittest.RunContext) -> T): T

    func abortCurrentStep: std.core.Option<std.unittest.RunStepResult>

    func collectUnattachedFailures: std.collection.ArrayList<std.unittest.CheckResult>

    func onTestCasesRegistered(packageName: std.core.String, casesCount: UInt64): Unit

    func onLifecycleStart(suiteId: std.unittest.TestSuiteId, suiteInfo: std.unittest.TestSuiteReportInfo, step: std.unittest.LStep): Unit

    func onTestCaseStarted(caseId: std.unittest.TestCaseId, caseInfo: std.unittest.TestCaseReportInfo, suiteInfo: std.unittest.TestSuiteReportInfo): Unit

    func onFinished(result: std.unittest.TestCaseResult): Unit

    func initWorker(ctx: std.unittest.MainExecutionCtx, workerId: Int64): std.unittest.WorkerProcess
}

enum Transport <: std.core.Resource {
    | TChannel(std.unittest.TransportChannel)

    | TMain(std.unittest.TransportMain)

    func close: Unit

    func isClosed: Bool

    prop channel: std.core.Option<std.unittest.TransportChannel>

    prop manager: std.core.Option<std.unittest.TransportMain>

    func fromDefaultConfiguration: std.unittest.Transport
}

class RunContext {
    let checkResults: std.collection.ArrayList<std.unittest.CheckResult>

    var suppressed: Bool

    var passedChecks: Int64

    func checkFailed(c: std.unittest.CheckResult): Unit

    func onException(e: std.core.Exception): Unit

    func storeCheckResult(check: std.unittest.CheckResult): Unit

    func checkPassed: Unit

    func suppressIfHasFailures: Unit

    func hasFailures: Bool

    init() { /* compiled code */ }
}

enum StepKind {
    | Skip

    | NoRun

    | Lifecycle(std.unittest.LStep)

    | CaseStep(std.unittest.ArgumentDescription)

    | UserCode

    func isClassLifecycle: Bool
}

enum StepInfo {
    | Test(Int64)

    | Bench(std.unittest.BenchmarkResult)

    | Failure(std.core.Array<std.unittest.CheckResult>)
}

class OutsideOfFrameworkContext <: std.unittest.RunContext {
    func storeCheckResult(_: std.unittest.CheckResult): Unit

    init() { /* compiled code */ }
}

class FrameworkInternalsContext <: std.unittest.RunContext {
    init() { /* compiled code */ }
}

class RunStepContext <: std.unittest.RunContext {
    var startTime: std.time.DateTime

    func checkFailed(c: std.unittest.CheckResult): Unit

    func finishStep: std.unittest.RunStepResult

    var stepKind: std.unittest.StepKind

    var stepInfo: std.unittest.StepInfo

    init(stepKind: std.unittest.StepKind, stepInfo: std.unittest.StepInfo) { /* compiled code */ }
}

class BenchmarkStoppedError <: std.core.Exception {
    init() { /* compiled code */ }
}

struct UInt8Entry {
    let left: UInt8

    let right: UInt8

    init(left: UInt8, right: UInt8) { /* compiled code */ }
}

struct UInt16Entry {
    let left: UInt16

    let right: UInt16

    init(left: UInt16, right: UInt16) { /* compiled code */ }
}

struct UInt32Entry {
    let left: UInt32

    let right: UInt32

    init(left: UInt32, right: UInt32) { /* compiled code */ }
}

struct UInt64Entry {
    let left: UInt64

    let right: UInt64

    init(left: UInt64, right: UInt64) { /* compiled code */ }
}

struct Float32Entry {
    let left: Float32

    let right: Float32

    init(left: Float32, right: Float32) { /* compiled code */ }
}

struct Float64Entry {
    let left: Float64

    let right: Float64

    init(left: Float64, right: Float64) { /* compiled code */ }
}

func CJ_MICROFUZZ_FLIP_BIT_FLOAT(value: Float32, bit: UInt8): Float32

func CJ_MICROFUZZ_FLIP_BIT_DOUBLE(value: Float64, bit: UInt8): Float64

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT8(result: CPointer<std.unittest.UInt8Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT16(result: CPointer<std.unittest.UInt16Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT32(result: CPointer<std.unittest.UInt32Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_UINT64(result: CPointer<std.unittest.UInt64Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FLOAT(result: CPointer<std.unittest.Float32Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_DOUBLE(result: CPointer<std.unittest.Float64Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT8(key: UInt8, result: CPointer<std.unittest.UInt8Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT16(key: UInt16, result: CPointer<std.unittest.UInt16Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT32(key: UInt32, result: CPointer<std.unittest.UInt32Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_UINT64(key: UInt64, result: CPointer<std.unittest.UInt64Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_FLOAT(key: Float32, result: CPointer<std.unittest.Float32Entry>): Unit

func CJ_MICROFUZZ_RANDOM_RECENT_CMP_VALUE_FOR_KEY_DOUBLE(key: Float64, result: CPointer<std.unittest.Float64Entry>): Unit

func CJ_MICROFUZZ_ENABLE_TRACE: Unit

func CJ_MICROFUZZ_DISABLE_TRACE: Unit

func CJ_MICROFUZZ_RESET_LOCAL_COVERAGE_INFO: Unit

func CJ_MICROFUZZ_RESET_ALL_COVERAGE_INFO: Unit

func CJ_MICROFUZZ_HAS_NEW_COVERAGE: Bool

func CJ_MICROFUZZ_IS_INITIALIZED: Bool

func CJ_MICROFUZZ_GLOBAL_EDGE_COVERAGE_EST: Float64

func CJ_MICROFUZZ_SET_DBG_CALLBACK(callback: (CString, CString, UInt64, UInt64) -> Unit): Unit

func printMessage(mes: CString, t: CString, v1: UInt64, v2: UInt64): Unit

func currentCoverageEst: Float64

interface Torcable {
    func randomRecentEntry: std.core.Option<(T, T)>

    func randomRecentValueComparedTo(value: T): std.core.Option<T>

    func nextFrom(random: std.random.Random): T

    func suggestedFrom(random: std.random.Random): T

    func isZero: Bool

    func bitFlip(at: UInt8): T

    operator func +(_: UInt8): T

    operator func -(_: UInt8): T
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(UInt8, UInt8)>

    func randomRecentValueComparedTo(value: UInt8): std.core.Option<UInt8>

    func nextFrom(random: std.random.Random): UInt8

    func suggestedFrom(random: std.random.Random): UInt8

    func isZero: Bool

    func bitFlip(at: UInt8): UInt8
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(UInt16, UInt16)>

    func randomRecentValueComparedTo(value: UInt16): std.core.Option<UInt16>

    func nextFrom(random: std.random.Random): UInt16

    func suggestedFrom(random: std.random.Random): UInt16

    func isZero: Bool

    func bitFlip(at: UInt8): UInt16

    operator func +(arg: UInt8): UInt16

    operator func -(arg: UInt8): UInt16
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(UInt32, UInt32)>

    func randomRecentValueComparedTo(value: UInt32): std.core.Option<UInt32>

    func nextFrom(random: std.random.Random): UInt32

    func suggestedFrom(random: std.random.Random): UInt32

    func isZero: Bool

    func bitFlip(at: UInt8): UInt32

    operator func +(arg: UInt8): UInt32

    operator func -(arg: UInt8): UInt32
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(UInt64, UInt64)>

    func randomRecentValueComparedTo(value: UInt64): std.core.Option<UInt64>

    func nextFrom(random: std.random.Random): UInt64

    func suggestedFrom(random: std.random.Random): UInt64

    func isZero: Bool

    func bitFlip(at: UInt8): UInt64

    operator func +(arg: UInt8): UInt64

    operator func -(arg: UInt8): UInt64
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(Float32, Float32)>

    func randomRecentValueComparedTo(value: Float32): std.core.Option<Float32>

    func nextFrom(random: std.random.Random): Float32

    func suggestedFrom(random: std.random.Random): Float32

    func isZero: Bool

    func bitFlip(at: UInt8): Float32

    operator func +(arg: UInt8): Float32

    operator func -(arg: UInt8): Float32
}

extend Extend <: Torcable {
    func randomRecentEntry: std.core.Option<(Float64, Float64)>

    func randomRecentValueComparedTo(value: Float64): std.core.Option<Float64>

    func nextFrom(random: std.random.Random): Float64

    func suggestedFrom(random: std.random.Random): Float64

    func isZero: Bool

    func bitFlip(at: UInt8): Float64

    operator func +(arg: UInt8): Float64

    operator func -(arg: UInt8): Float64
}

enum Mutation {
    | Drop

    | Insert(T)

    | ReplaceWith(T)

    | BitFlip(UInt8)

    | Add(UInt8)

    | Sub(UInt8)

    func apply(at: Int64, values: std.collection.ArrayList<T>): Unit
}

class Mutator {
    func randomRecentValue: std.core.Option<T>

    func randomRecentCmp(value: T): std.core.Option<T>

    func randomMutation(temperature: UInt8, value: T): std.core.Option<std.unittest.Mutation<T>>

    let random: std.random.Random

    init(random: std.random.Random) { /* compiled code */ }
}

class MutatingRandomValueStream {
    var currentIndex: Int64

    func nextUniform(random: std.random.Random): T

    func next(random: std.random.Random, temperature: UInt8, mutator: std.unittest.Mutator): T

    func reset: std.unittest.MutatingRandomValueStream<T>

    func freeze: std.unittest.MutatingRandomValueStream<T>

    let values: std.collection.ArrayList<T>

    init(values: std.collection.ArrayList<T>) { /* compiled code */ }
}

class CovRandomSource <: std.core.Object & std.unittest.prop_test.RandomSource {
    let inner: std.random.Random

    init(seed: UInt64, temperature: UInt8, mutator: std.unittest.Mutator) { /* compiled code */ }

    func copy(seed: UInt64, temperature: UInt8): std.unittest.CovRandomSource

    func freeze: std.unittest.CovRandomSource

    func suggestBool: Bool

    func suggestUInt8: UInt8

    func suggestUInt16: UInt16

    func suggestUInt32: UInt32

    func suggestUInt64: UInt64

    func suggestInt8: Int8

    func suggestInt16: Int16

    func suggestInt32: Int32

    func suggestInt64: Int64

    func nextFloat16: Float16

    func nextFloat32: Float32

    func nextFloat64: Float64

    func suggestFloat16: Float16

    func suggestFloat32: Float32

    func suggestFloat64: Float64

    var gaussian: std.unittest.GaussianGenerator

    func nextGaussianFloat64(mean: Float64, sigma: Float64): Float64

    func nextUInt8: UInt8

    func nextUInt16: UInt16

    func nextUInt32: UInt32

    func nextUInt64: UInt64

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

    func next(random: std.unittest.prop_test.RandomSource): Float64

    let mean: Float64

    let sigma: Float64

    init(mean: Float64, sigma: Float64) { /* compiled code */ }
}

class CoverageInfo {
    func newCoverageDiscovered: Bool

    init() { /* compiled code */ }
}

func printUnittestHelpPageIfRequested: Bool

struct KeyHelp <: std.unittest.common.KeyFor<Bool> {
    prop help: std.unittest.KeyHelp

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyNoColor <: std.unittest.common.KeyFor<Bool> {
    prop noColor: std.unittest.KeyNoColor

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyFromTopLevel <: std.unittest.common.KeyFor<Bool> {
    prop fromTopLevel: std.unittest.KeyFromTopLevel

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyRandomSeed <: std.unittest.common.KeyFor<Int64> {
    prop randomSeed: std.unittest.KeyRandomSeed

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyWarmup <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Duration> {
    prop warmup: std.unittest.KeyWarmup

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyMinDuration <: std.unittest.common.KeyFor<std.core.Duration> {
    prop minDuration: std.unittest.KeyMinDuration

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyExplicitGC <: std.unittest.common.KeyFor<std.unittest.ExplicitGcType> {
    prop explicitGC: std.unittest.KeyExplicitGC

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyMinBatches <: std.unittest.common.KeyFor<Int64> {
    prop minBatches: std.unittest.KeyMinBatches

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyBatchSize <: std.unittest.common.KeyFor<Int64> & std.unittest.common.KeyFor<std.core.Range<Int64>> {
    prop batchSize: std.unittest.KeyBatchSize

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyParallel <: std.unittest.common.KeyFor<Bool> & std.unittest.common.KeyFor<std.core.String> & std.unittest.common.KeyFor<Int64> {
    prop parallel: std.unittest.KeyParallel

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyGenerationSteps <: std.unittest.common.KeyFor<Int64> {
    prop generationSteps: std.unittest.KeyGenerationSteps

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyReductionSteps <: std.unittest.common.KeyFor<Int64> {
    prop reductionSteps: std.unittest.KeyReductionSteps

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeySkip <: std.unittest.common.KeyFor<Bool> {
    prop skip: std.unittest.KeySkip

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuided <: std.unittest.common.KeyFor<Bool> {
    prop coverageGuided: std.unittest.KeyCoverageGuided

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedInitialSeeds <: std.unittest.common.KeyFor<Int64> {
    prop coverageGuidedInitialSeeds: std.unittest.KeyCoverageGuidedInitialSeeds

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedMaxCandidates <: std.unittest.common.KeyFor<Int64> {
    prop coverageGuidedMaxCandidates: std.unittest.KeyCoverageGuidedMaxCandidates

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedBaselineScore <: std.unittest.common.KeyFor<Int64> {
    prop coverageGuidedBaselineScore: std.unittest.KeyCoverageGuidedBaselineScore

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedNewCoverageScore <: std.unittest.common.KeyFor<Int64> {
    prop coverageGuidedNewCoverageScore: std.unittest.KeyCoverageGuidedNewCoverageScore

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCoverageGuidedNewCoverageBonus <: std.unittest.common.KeyFor<Int64> {
    prop coverageGuidedNewCoverageBonus: std.unittest.KeyCoverageGuidedNewCoverageBonus

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyBench <: std.unittest.common.KeyFor<Bool> {
    prop bench: std.unittest.KeyBench

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyTimeout <: std.unittest.common.KeyFor<std.core.Duration> {
    prop timeout: std.unittest.KeyTimeout

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyTimeoutEach <: std.unittest.common.KeyFor<std.core.String> {
    prop timeoutEach: std.unittest.KeyTimeoutEach

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyTimeoutHandler <: std.unittest.common.KeyFor<(std.unittest.TestCaseInfo) -> Unit> {
    prop timeoutHandler: std.unittest.KeyTimeoutHandler

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    prop captureOutput: std.unittest.KeyCaptureOutput

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyNoCaptureOutput <: std.unittest.common.KeyFor<Bool> {
    prop noCaptureOutput: std.unittest.KeyNoCaptureOutput

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyShowAllOutput <: std.unittest.common.KeyFor<Bool> {
    prop showAllOutput: std.unittest.KeyShowAllOutput

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyVerbose <: std.unittest.common.KeyFor<Bool> {
    prop verbose: std.unittest.KeyVerbose

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyMeasurement <: std.unittest.common.KeyFor<std.unittest.Measurement> {
    prop measurement: std.unittest.KeyMeasurement

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyMeasurementInfo <: std.unittest.common.KeyFor<std.unittest.MeasurementInfo> {
    prop measurementInfo: std.unittest.KeyMeasurementInfo

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyInternalTestrunnerInputPath <: std.unittest.common.KeyFor<std.core.String> {
    prop internalTestrunnerInputPath: std.unittest.KeyInternalTestrunnerInputPath

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyDeathAware <: std.unittest.common.KeyFor<Bool> {
    prop deathAware: std.unittest.KeyDeathAware

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyBaseline <: std.unittest.common.KeyFor<std.core.String> {
    prop baseline: std.unittest.KeyBaseline

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyFilter <: std.unittest.common.KeyFor<std.core.String> {
    prop filter: std.unittest.KeyFilter

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyIncludeTags <: std.unittest.common.KeyFor<std.core.String> {
    prop includeTags: std.unittest.KeyIncludeTags

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyExcludeTags <: std.unittest.common.KeyFor<std.core.String> {
    prop excludeTags: std.unittest.KeyExcludeTags

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyBaselinePath <: std.unittest.common.KeyFor<std.core.String> {
    prop baselinePath: std.unittest.KeyBaselinePath

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyInternalWorkerId <: std.unittest.common.KeyFor<Int64> {
    prop internalWorkerId: std.unittest.KeyInternalWorkerId

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyInternalNWorkers <: std.unittest.common.KeyFor<Int64> {
    prop internalNWorkers: std.unittest.KeyInternalNWorkers

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyInternalWorkerSkipNTests <: std.unittest.common.KeyFor<Int64> {
    prop internalWorkerSkipNTests: std.unittest.KeyInternalWorkerSkipNTests

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyInternalMainProcessPort <: std.unittest.common.KeyFor<UInt16> {
    prop internalMainProcessPort: std.unittest.KeyInternalMainProcessPort

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyDryRun <: std.unittest.common.KeyFor<Bool> {
    prop dryRun: std.unittest.KeyDryRun

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyShowTags <: std.unittest.common.KeyFor<Bool> {
    prop showTags: std.unittest.KeyShowTags

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyNoProgress <: std.unittest.common.KeyFor<Bool> {
    prop noProgress: std.unittest.KeyNoProgress

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyProgressBrief <: std.unittest.common.KeyFor<Bool> {
    prop progressBrief: std.unittest.KeyProgressBrief

    prop name: std.core.String

    init() { /* compiled code */ }
}

struct KeyProgressEntriesLimit <: std.unittest.common.KeyFor<Int64> {
    prop progressEntriesLimit: std.unittest.KeyProgressEntriesLimit

    prop name: std.core.String

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

    func fromDefaultConfiguration: std.unittest.ParallelInfo

    prop nWorkers: Int64
}

class PowerAssertValue <: std.core.Object & std.unittest.common.Serializable<std.unittest.PowerAssertValue> {
    init(value: std.core.String, expression: std.core.String, position: Int64, isException: Bool) { /* compiled code */ }

    prop value: std.core.String

    prop range: std.core.Range<Int64>

    prop valuePosition: Int64

    prop safeValueNextPosition: Int64

    func skipped(expression: std.core.String, position: Int64): std.unittest.PowerAssertValue

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValue

    let kind: std.unittest.PowerAssertValueKind

    let expression: std.core.String

    let position: Int64

    init(kind: std.unittest.PowerAssertValueKind, expression: std.core.String, position: Int64) { /* compiled code */ }
}

enum PowerAssertValueKind <: std.unittest.common.Serializable<std.unittest.PowerAssertValueKind> {
    | PAValue(std.core.String)

    | PAThrown(std.core.String)

    | PASkipped

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.PowerAssertValueKind
}

class PowerAssertDiagramBuilder {
    var wasExceptionThrown: Bool

    let expression: std.core.String

    let diagram: std.unittest.PowerAssertDiagram

    init(expression: std.core.String) { /* compiled code */ }

    func r(value: T, exprAsText: std.core.String, position: Int64): T

    func r(value: Rune, exprAsText: std.core.String, position: Int64): Rune

    func r(value: std.core.String, exprAsText: std.core.String, position: Int64): std.core.String

    func h(exception: std.core.Exception, exprAsText: std.core.String, position: Int64): Nothing

    func w(passed: Bool): Unit
}

func EnableANSITerminal: Bool

func DisableANSITerminal: Bool

var isConsoleUtf8: Bool

class TerminalPrettyPrinter <: std.unittest.common.PrettyPrinter {
    let areColorsEnabled: Bool

    let areOtherAnsiEnabled: Bool

    init(configuration: std.unittest.common.Configuration) { /* compiled code */ }

    prop terminalHeight: std.core.Option<UInt64>

    prop terminalWidth: std.core.Option<UInt64>

    func exclusive(action: (std.unittest.TerminalPrettyPrinter) -> T): T

    func up(lines: Int64): Unit

    func down(lines: Int64): Unit

    func toBottom: Unit

    func setColumn(column: Int64): Unit

    func saveCursorPos: Unit

    func restoreCursorPos: Unit

    func hideCursor: Unit

    func showCursor: Unit

    func setScrollableMargins(start: std.core.Option<Int64>, end: std.core.Option<Int64>): Unit

    func clearAhead: Unit

    func clearLine: Unit

    func put(s: std.core.String): Unit

    func flush: Unit

    func setColor(color: std.unittest.common.Color): Unit

    prop instance: std.unittest.TerminalPrettyPrinter

    func fillLimitedSpace(spaceSize: Int64, body: () -> Unit): std.unittest.common.PrettyPrinter

    func fromDefaultConfiguration: std.unittest.TerminalPrettyPrinter

    init(isColored: Bool, isOtherAnsiAble: Bool) { /* compiled code */ }
}

func SetConsoleOutputCP(cp: UInt32): UInt32

func setUtf8OutputCP: Bool

func GetTerminalWidth: UInt64

func GetTerminalHeight: UInt64

class CapturedProcessResult {
    let stdOut: std.core.Array<UInt8>

    let stdErr: std.core.Array<UInt8>

    init(stdOut: std.core.Array<UInt8>, stdErr: std.core.Array<UInt8>) { /* compiled code */ }
}

func processStart(ctx: std.unittest.MainExecutionCtx): std.unittest.WrappedProcess

func renderException(exception: std.core.Exception, preamble: std.core.Option<std.unittest.common.PrettyText>): std.unittest.common.PrettyText

func exceptionToString(e: std.core.Exception): std.core.String

func indexOfFirstIrrelevantEntry(elements: std.core.Array<std.core.StackTraceElement>): Int64

func isEntryPoint(_: std.core.StackTraceElement): Bool

func isLambda(element: std.core.StackTraceElement): Bool

func methodName(element: std.core.StackTraceElement): std.core.String

extend TestGroupResult {
    func reportCsv(dir: std.fs.Path, raw: Bool): Unit
}

extend TestSuiteResult {
    func reportBenchCsv(raw: Bool): std.core.ToString
}

let STYLES: std.core.String

let CASE_TEMPLATE: std.core.String

let ROOT_TEMPLATE: std.core.String

var gnuplotAvailable: Bool

extend TestGroupResult {
    func reportHtml(dir: std.fs.Path): Unit

    func collectClassObjects(passedFQNames: std.core.Iterable<std.unittest.TestCaseId>): std.collection.ArrayList<std.unittest.common.JsonObject>

    func envRow(name: std.core.String, value: T): std.unittest.common.JsonObject

    func collectEnvironment: std.unittest.common.JsonArray

    func tryGetNative(f: () -> std.core.String): std.core.String

    func collectAllResults(directory: std.fs.Path): std.collection.HashMap<std.unittest.TestCaseId, std.unittest.common.JsonValue>
}

func measurementModel(step: std.unittest.BenchStepEntry, directory: std.fs.Path, index: Int64): (Float64, std.unittest.common.JsonObject)

struct BenchStepEntry {
    let result: std.unittest.BenchmarkResult

    let arg: std.unittest.ArgumentDescription

    let caseId: std.unittest.TestCaseId

    let measurementInfo: std.unittest.MeasurementInfo

    init(result: std.unittest.BenchmarkResult, arg: std.unittest.ArgumentDescription, caseId: std.unittest.TestCaseId, measurementInfo: std.unittest.MeasurementInfo) { /* compiled code */ }
}

extend StatisticsSample {
    func createStatsRow(name: std.core.String, dir: std.fs.Path, change: std.core.Option<Float64>): std.unittest.common.JsonObject

    func drawKde(into: std.fs.Path): Unit
}

extend BenchmarkResult {
    func draw(into: std.fs.Path, mName: std.core.String): Unit
}

let NO_INFO: std.core.String

func getOsVersion: std.core.String

func getCpuName: std.core.String

func getCompilerVersion: std.core.String

func getTargetTriple: std.core.String

enum TestCode <: std.core.ToString & std.unittest.common.PrettyPrintable & std.core.Equatable<std.unittest.TestCode> {
    | PASS

    | SKIP

    | TIMEOUT

    | FAIL

    | ERROR

    | NORUN

    func toString: std.core.String

    func mostSevereWith(other: std.unittest.TestCode): std.unittest.TestCode

    func isFailure: Bool

    func toStringTense: std.core.String

    operator func ==(right: std.unittest.TestCode): Bool

    operator func !=(right: std.unittest.TestCode): Bool

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

class XmlBuilder <: std.core.Object & std.core.ToString {
    let element_: std.unittest.XmlElementBuilder

    var version_: std.core.ToString

    var encoding_: std.core.Option<std.core.ToString>

    func version(v: std.core.ToString): std.unittest.XmlBuilder

    func encoding(format: std.core.ToString): std.unittest.XmlBuilder

    func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlBuilder

    func toString: std.core.String

    init() { /* compiled code */ }
}

class XmlElementBuilder <: std.core.Object & std.core.ToString {
    var name_: std.core.ToString

    var attributes_: std.collection.ArrayList<(std.core.ToString, std.core.ToString)>

    var children: std.collection.ArrayList<std.unittest.XmlElementBuilder>

    var content_: std.core.ToString

    prop attributes: std.collection.ArrayList<(std.core.ToString, std.core.ToString)>

    prop content: std.core.ToString

    func name(name: std.core.ToString): std.unittest.XmlElementBuilder

    func attribute(key: std.core.ToString, value: std.core.ToString): std.unittest.XmlElementBuilder

    func optionalAttribute(key: std.core.ToString, value: std.core.Option<std.core.String>): std.unittest.XmlElementBuilder

    func element(contentBuilder: () -> std.core.ToString): std.unittest.XmlElementBuilder

    func element(name: std.core.ToString, childBuilder: (std.unittest.XmlElementBuilder) -> Unit): std.unittest.XmlElementBuilder

    func build(sb: std.core.StringBuilder): Unit

    func toString: std.core.String

    let level: UInt64

    init(level: UInt64) { /* compiled code */ }
}

extend TestSuiteId {
    func asClassName: std.core.String
}

extend TestGroupResult {
    func reportXml(dir: std.fs.Path): Unit
}

extend TestSuiteId {
    func reportFileName(isBench: Bool, extension: std.core.String): std.core.String
}

class CsvReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let directory: std.fs.Path

    init(directory: std.fs.Path) { /* compiled code */ }
}

class CsvRawReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let directory: std.fs.Path

    init(directory: std.fs.Path) { /* compiled code */ }
}

class XmlReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> {
    let directory: std.fs.Path

    init(directory: std.fs.Path) { /* compiled code */ }
}

class ConsoleReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, Unit> & std.unittest.Reporter<std.unittest.BenchReport, Unit> {
    let colored: Bool

    init(colored: Bool) { /* compiled code */ }
}

class TextReporter <: std.core.Object & std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    let into: PP

    init(into: PP) { /* compiled code */ }
}

interface TextReporterBase <: std.unittest.Reporter<std.unittest.TestReport, PP> & std.unittest.Reporter<std.unittest.BenchReport, PP> {
    func printReport(report: std.unittest.Report): PP
}

extend TextReporter <: TextReporterBase {
    func printReport(report: std.unittest.Report): PP
}

extend ConsoleReporter <: TextReporterBase {
    func printReport(report: std.unittest.Report): Unit
}

class RawStatsReporter <: std.core.Object & std.unittest.Reporter<std.unittest.BenchReport, std.collection.HashMap<std.core.String, (Float64, Float64)>> {
    func benchResultsAsMap(result: std.unittest.TestGroupResult): std.collection.HashMap<std.core.String, (Float64, Float64)>

    init() { /* compiled code */ }
}

class ProgressReporter {
    let reporter: std.unittest.UTProgressReporter

    var terminalHeight: std.core.Option<UInt64>

    var terminalWidth: std.core.Option<UInt64>

    var prevReportHeight: Int64

    let printer: std.unittest.TerminalPrettyPrinter

    var reporterJob: std.core.Option<std.core.Future<Unit>>

    let isActive: std.sync.AtomicBool

    prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    func startReporting: Unit

    func stopAndClear: Unit

    func fromDefaultConfiguration: std.core.Option<std.unittest.ProgressReporter>
}

interface UTProgressReporter {
    prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    func start: Unit

    func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText

    func reset: Unit
}

class TestProgressReporter <: std.core.Object & std.unittest.UTProgressReporter {
    var width: Int64

    var height: Int64

    var markWidth: Int64

    let booker: std.unittest.TestProgressCollector

    prop updateQueue: std.collection.concurrent.ConcurrentLinkedQueue<std.unittest.UTProgress>

    func start: Unit

    func report(terminalHeight: std.core.Option<UInt64>, terminalWidth: std.core.Option<UInt64>): std.unittest.common.PrettyText

    let testCasesParallelism: Int64

    let isBrief: Bool

    let entriesLimit: std.core.Option<UInt64>

    init(testCasesParallelism: Int64, isBrief: Bool, entriesLimit: std.core.Option<UInt64>) { /* compiled code */ }
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

    prop totalCasesCount: UInt64

    prop finishedCasesCount: UInt64

    prop passedCasesCount: UInt64

    prop failedCasesCount: UInt64

    prop crashedWorkersCount: UInt64

    prop registeredPackagesCount: UInt64

    prop currentPackagesCount: UInt64

    prop lastFailedStep: std.core.Option<std.unittest.FailedTestCaseProgressData>

    prop currentPackages: std.core.Array<std.unittest.PackageProgressData>

    prop currentTestCases: std.core.Array<std.unittest.TestCaseProgressData>

    prop summary: std.unittest.SummaryProgressData

    func start: Unit

    func flushChanges: Unit

    init(testCasesParallelism: Int64) { /* compiled code */ }
}

struct UniqueWorkerId <: std.core.Hashable & std.core.Equatable<std.unittest.UniqueWorkerId> {
    func hashCode: Int64

    operator func ==(other: std.unittest.UniqueWorkerId): Bool

    operator func !=(other: std.unittest.UniqueWorkerId): Bool

    let id: Int64

    let pack: std.core.String

    init(id: Int64, pack: std.core.String) { /* compiled code */ }
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

    func update(positive: Bool): Unit

    func expand(extendBy: UInt64): Unit

    func pprint(to: std.unittest.common.PrettyPrinter, crashedWorkers: UInt64, width: Int64, mainBar: Bool): std.unittest.common.PrettyPrinter

    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

    var totalPieces: Int64

    init(totalPieces: Int64) { /* compiled code */ }
}

struct FailedTestCaseProgressData {
    func trimmed(limit: Int64): std.core.String

    let name: std.unittest.TestCaseName

    let suiteId: std.unittest.TestSuiteId

    let check: std.unittest.RunStepResult

    init(name: std.unittest.TestCaseName, suiteId: std.unittest.TestSuiteId, check: std.unittest.RunStepResult) { /* compiled code */ }
}

struct TestCaseProgressData {
    prop duration: std.core.Duration

    func trimmed(limit: Int64): std.core.String

    func trim(suiteId: std.unittest.TestSuiteId, name: std.unittest.TestCaseName, limit: Int64): std.core.String

    let name: std.unittest.TestCaseName

    let suiteId: std.unittest.TestSuiteId

    let startTime: std.time.DateTime

    init(name: std.unittest.TestCaseName, suiteId: std.unittest.TestSuiteId, startTime: std.time.DateTime) { /* compiled code */ }
}

struct PackageProgressData {
    prop finishPercent: UInt64

    prop duration: std.core.Duration

    func trimmed(limit: Int64): std.core.String

    let name: std.core.String

    let totalCases: UInt64

    let finishedCases: UInt64

    let isFailed: Bool

    let startTime: std.time.DateTime

    let progress: std.unittest.Progress

    init(name: std.core.String, totalCases: UInt64, finishedCases: UInt64, isFailed: Bool, startTime: std.time.DateTime, progress: std.unittest.Progress) { /* compiled code */ }
}

struct SummaryProgressData {
    prop finishPercent: UInt64

    prop isFailed: Bool

    prop duration: std.core.Duration

    let passedCases: UInt64

    let failedCases: UInt64

    let finishedCases: UInt64

    let totalCases: UInt64

    let crashedWorkers: UInt64

    let startTime: std.time.MonoTime

    let progress: std.unittest.Progress

    init(passedCases: UInt64, failedCases: UInt64, finishedCases: UInt64, totalCases: UInt64, crashedWorkers: UInt64, startTime: std.time.MonoTime, progress: std.unittest.Progress) { /* compiled code */ }
}

struct TestProgressData <: std.unittest.UTProgress {
    let data: std.unittest.ExecutionResultPart

    let workerId: Int64

    init(data: std.unittest.ExecutionResultPart, workerId: Int64) { /* compiled code */ }
}

enum TestCaseName <: std.core.ToString {
    | CaseName(std.unittest.TestCaseId)

    | CNStep(std.unittest.LStep)

    func toString: std.core.String
}

extend ExecutionResultPart {
    func toCaseName: std.unittest.TestCaseName
}

extend ExecutionResultBody {
    prop isSkip: Bool

    prop isFailed: Bool

    prop firstFailedStep: std.unittest.RunStepResult

    prop isPassed: Bool

    prop isClassLifecycle: Bool

    prop isBeforeAll: Bool
}

extend TestCaseId {
    func toDisplayString: std.core.String
}

extend TestSuiteId {
    func toDisplayString: std.core.String
}

struct ArgumentDescription {
    func intoErrorMessage: std.core.Option<std.unittest.common.PrettyText>

    let textDescription: std.core.Option<std.unittest.common.PrettyText>

    let stepIndex: Int64

    let reductionIndex: Int64

    let randomSeed: std.core.Option<Int64>

    init(textDescription: std.core.Option<std.unittest.common.PrettyText>, stepIndex: Int64, reductionIndex: Int64, randomSeed: std.core.Option<Int64>) { /* compiled code */ }
}

struct RenderOptions {
    func fromConfiguration(configuration: std.unittest.common.Configuration): std.unittest.RenderOptions

    let baselineString: std.core.Option<std.core.String>

    let measurementInfo: std.core.Option<std.unittest.MeasurementInfo>

    init(baselineString: std.core.Option<std.core.String>, measurementInfo: std.core.Option<std.unittest.MeasurementInfo>) { /* compiled code */ }
}

enum GroupResultPart <: std.unittest.Result {
    | SuiteResult(std.unittest.TestSuiteResult)

    | ProcessError(std.unittest.TestProcessError)

    func walk(visitor: (std.unittest.Result) -> Unit): Unit

    func visitAll(visitor: (V) -> Unit): Unit

    prop details: std.unittest.Details
}

extend LStep <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.LStep
}

extend StepKind <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepKind
}

extend StepInfo <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.StepInfo
}

extend RunStepResult {
    func doSerialize: std.unittest.common.DataModelStruct

    func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.RunStepResult
}

extend TestCaseResult {
    func doSerialize: std.unittest.common.DataModelStruct

    func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseResult
}

extend TestCaseReportInfo <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestCaseReportInfo
}

extend TestSuiteResult {
    func doSerialize: std.unittest.common.DataModelStruct

    func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteResult
}

extend TestSuiteReportInfo <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.TestSuiteReportInfo
}

extend TestGroupResult {
    func doSerialize: std.unittest.common.DataModelStruct

    func doDeserialize(dm: std.unittest.common.DataModel): std.unittest.TestGroupResult
}

extend RenderOptions <: Serializable {
    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.RenderOptions
}

struct ArrayOfTuples <: std.unittest.common.Serializable<std.unittest.ArrayOfTuples<A, B>> {
    init(col: std.core.Collection<(A, B)>) { /* compiled code */ }

    func original: std.core.Array<(A, B)>

    func serializeInternal: std.unittest.common.DataModel

    func deserialize(dm: std.unittest.common.DataModel): std.unittest.ArrayOfTuples<A, B>

    let left: std.core.Array<A>

    let right: std.core.Array<B>

    init(left: std.core.Array<A>, right: std.core.Array<B>) { /* compiled code */ }
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

    func calculateMedian: std.unittest.Measurements

    func calcSlopes: Unit

    func calculateMean: std.unittest.Measurements

    let data: std.core.Array<(Float64, Float64)>

    init(data: std.core.Array<(Float64, Float64)>) { /* compiled code */ }
}

interface AsFloat {
    func asFloat: Float64
}

extend Extend <: AsFloat {
    func asFloat: Float64
}

class FlatMapProcessor <: std.unittest.DataStrategyProcessor<R> {
    var provider: std.unittest.RememberingDataProvider<R>

    prop isInfinite: Bool

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R>

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(_: std.unittest.common.Configuration): R

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T) -> std.unittest.common.DataProvider<R>

    init(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataProvider<R>) { /* compiled code */ }
}

class LazyCyclicNode {
    var next_: std.unittest.LazyCyclicNode

    var prev: std.unittest.LazyCyclicNode

    func initialize: std.unittest.LazyCyclicNode

    func advance: std.core.Option<Unit>

    func recover: Unit

    func add(other: std.unittest.LazyCyclicNode): Unit

    func removeSelf: std.unittest.LazyCyclicNode

    init() { /* compiled code */ }
}

class FlatMapStrategyProcessor <: std.unittest.DataStrategyProcessor<R> {
    var provider: std.unittest.RememberingDataProvider<R>

    var strategy: std.unittest.common.DataStrategy<R>

    prop isInfinite: Bool

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R>

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(_: std.unittest.common.Configuration): R

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T) -> std.unittest.common.DataStrategy<R>

    init(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T) -> std.unittest.common.DataStrategy<R>) { /* compiled code */ }
}

class MapProcessor <: std.unittest.DataStrategyProcessor<R> {
    prop isInfinite: Bool

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<R>

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<R>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(configuration: std.unittest.common.Configuration): R

    let orig: std.unittest.DataStrategyProcessor<T>

    let mapper: (T, std.unittest.common.Configuration) -> R

    init(orig: std.unittest.DataStrategyProcessor<T>, mapper: (T, std.unittest.common.Configuration) -> R) { /* compiled code */ }
}

class CartesianProductProcessor <: std.unittest.DataStrategyProcessor<(T0, T1)> {
    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<(T0, T1)>

    prop isInfinite: Bool

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<(T0, T1)>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(configuration: std.unittest.common.Configuration): (T0, T1)

    let left: std.unittest.DataStrategyProcessor<T0>

    let right: std.unittest.DataStrategyProcessor<T1>

    init(left: std.unittest.DataStrategyProcessor<T0>, right: std.unittest.DataStrategyProcessor<T1>) { /* compiled code */ }
}

class CartesianProductIterator <: std.core.Iterator<(A, B)> {
    var leftElement: std.core.Option<A>

    var right: std.core.Iterator<B>

    func next: std.core.Option<(A, B)>

    let left: std.core.Iterator<A>

    let rightGen: () -> std.core.Iterator<B>

    init(left: std.core.Iterator<A>, rightGen: () -> std.core.Iterator<B>) { /* compiled code */ }
}

class SimpleProcessor <: std.unittest.DataStrategyProcessor<T> {
    var dataProvider: std.unittest.RememberingDataProvider<T>

    var _delegate: std.core.Option<std.unittest.common.DataStrategy<T>>

    prop strategy: std.unittest.common.DataStrategy<T>

    prop isInfinite: Bool

    func provide(configuration: std.unittest.common.Configuration): std.core.Iterable<T>

    func shrinkLastItem(configuration: std.unittest.common.Configuration, engine: std.unittest.LazyCyclicNode): std.core.Iterable<T>

    func lastItemInfo: std.core.Array<std.unittest.InputParameter>

    func lastItem(_: std.unittest.common.Configuration): T

    let buildDelegate: () -> std.unittest.common.DataStrategy<T>

    let name: std.core.String

    init(buildDelegate: () -> std.unittest.common.DataStrategy<T>, name: std.core.String) { /* compiled code */ }
}

class RememberingIterable <: std.core.Iterator<T> {
    func next: std.core.Option<T>

    let delegate: std.core.Iterator<T>

    let source: std.unittest.RememberingDataProvider<T>

    init(delegate: std.core.Iterator<T>, source: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

class RememberingLazyIterator <: std.core.Iterator<T> {
    func next: std.core.Option<T>

    let delegate: std.unittest.RememberingLazyIterable<T>

    init(delegate: std.unittest.RememberingLazyIterable<T>) { /* compiled code */ }
}

class RememberingLazyIterable <: std.unittest.LazyCyclicNode & std.core.Iterable<T> {
    var currentItem: std.core.Option<T>

    var origLastItem: T

    func recover: Unit

    func advance: std.core.Option<Unit>

    func iterator: std.core.Iterator<T>

    let delegate: std.core.Iterator<T>

    let storage: std.unittest.RememberingDataProvider<T>

    init(delegate: std.core.Iterator<T>, storage: std.unittest.RememberingDataProvider<T>) { /* compiled code */ }
}

class IterableMix <: std.core.Iterator<T> {
    func next: std.core.Option<T>

    let delegate1: std.core.Iterator<T>

    let delegate2: std.core.Iterator<T>

    init(delegate1: std.core.Iterator<T>, delegate2: std.core.Iterator<T>) { /* compiled code */ }
}

class LazyCyclicNodeEngine <: std.core.Iterator<T> {
    func next: std.core.Option<T>

    func advance: std.core.Option<Unit>

    var current: std.unittest.LazyCyclicNode

    let iter: std.core.Iterator<T>

    init(current: std.unittest.LazyCyclicNode, iter: std.core.Iterator<T>) { /* compiled code */ }
}

type ShrinkResult = std.unittest.RunStepResult

enum ProcessorResult {
    | Success(std.unittest.RunStepResult)

    | Failure(std.core.Array<std.unittest.InputParameter>, std.unittest.RunStepResult)

    | NoItems

    | StrategyError(std.core.Exception)
}

extend DataStrategyProcessor {
    func map(f: (T) -> R): std.unittest.MapProcessor<T, R>

    func mapWithConfig(f: (T, std.unittest.common.Configuration) -> R): std.unittest.MapProcessor<T, R>

    func flatMap(f: (T) -> std.unittest.common.DataProvider<R>): std.unittest.FlatMapProcessor<T, R>

    func flatMapStrategy(f: (T) -> std.unittest.common.DataStrategy<R>): std.unittest.FlatMapStrategyProcessor<T, R>

    func product(p: std.unittest.DataStrategyProcessor<R>): std.unittest.CartesianProductProcessor<T, R>

    func productWithUnit(p: P): std.unittest.MapProcessor<(T, Unit), T>
}

class TestSuiteExecutor {
    let suiteConfiguration: std.unittest.common.Configuration

    let suiteInfo: std.unittest.TestSuiteReportInfo

    func execute: std.unittest.TestSuiteResult

    func runSkip(testCaseResult: std.unittest.TestCaseResult): Unit

    func runNoRun(testCaseResult: std.unittest.TestCaseResult): Unit

    func runSingleCase(caseConfiguration: std.unittest.common.Configuration, executor: std.unittest.Executor): (std.unittest.TestCaseResult) -> Unit

    let externalConfiguration: std.unittest.common.Configuration

    let suiteId: std.unittest.TestSuiteId

    let testSuite: std.unittest.TestSuite

    let filterService: std.unittest.FilterService

    init(externalConfiguration: std.unittest.common.Configuration, suiteId: std.unittest.TestSuiteId, testSuite: std.unittest.TestSuite, filterService: std.unittest.FilterService) { /* compiled code */ }
}

extend TestSuite {
    prop wholeConfiguration: std.unittest.common.Configuration

    prop allCasesIterator: std.core.Iterable<std.unittest.CaseOrBench>

    func allCasesToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): std.core.Array<std.unittest.CaseOrBench>

    func casesCountToBeExecuted(filterService: std.unittest.FilterService, suiteId: std.unittest.TestSuiteId): Int64

    func runBeforeEach(caseShortName: std.core.String): Unit

    func runAfterEach(caseShortName: std.core.String): Unit

    func runBeforeAll: Unit

    func runAfterAll: Unit
}

class TempDirectory {
    let lock: std.sync.ReentrantMutex

    let rnd: std.random.Random

    let buffer: std.core.Array<Rune>

    var currentOffset: Int64

    var tempDir: std.core.Option<std.fs.Path>

    prop path: std.fs.Path

    func createTempFile(dir: std.fs.Path, prefix: std.core.String, suffix: std.core.String): std.fs.File

    func createTempPath(dir: std.fs.Path, prefix: std.core.String, suffix: std.core.String): std.fs.Path

    init() { /* compiled code */ }
}

func substituteTemplate(template: std.core.String, values: std.unittest.common.JsonValue): std.core.String

class Root {
    var templates: std.collection.HashMap<std.core.String, std.unittest.Template>

    var rootTemplate: std.unittest.Template

    func substitute(values: std.unittest.common.JsonValue): std.core.String

    init(t: std.core.String) { /* compiled code */ }
}

class Template {
    func fromString(text: std.core.String): std.unittest.Template

    var REPEATABLES_MATCHER: std.regex.Regex

    var VARIABLES_MATCHER: std.regex.Regex

    let REPETITIONS_SANITY_CHECK: Int64

    func substitute(values: std.unittest.common.JsonValue, templates: std.collection.HashMap<std.core.String, std.unittest.Template>, index: Int64): std.core.Option<std.core.String>

    let name: std.core.String

    let baseText: std.core.String

    init(name: std.core.String, baseText: std.core.String) { /* compiled code */ }
}

func object(data: std.core.Array<(std.core.String, std.unittest.IntoJson)>): std.unittest.common.JsonObject

interface IntoJson {
    func json: std.unittest.common.JsonValue
}

extend JsonValue <: IntoJson {
    func json: std.unittest.common.JsonValue
}

extend String <: IntoJson {
    func json: std.unittest.common.JsonValue
}

extend Extend <: IntoJson {
    func json: std.unittest.common.JsonValue
}

extend Option <: IntoJson {
    func json: std.unittest.common.JsonValue
}

extend Array <: IntoJson {
    func json: std.unittest.common.JsonValue
}

extend ArrayList <: IntoJson {
    func json: std.unittest.common.JsonValue
}

func runTestStep(caseId: std.unittest.TestCaseId, f: () -> Unit): Unit

class TestCaseExecutor <: std.core.Object & std.unittest.Executor {
    var generationStep: Int64

    var randomSeed: std.core.Option<Int64>

    var hasMoreParameters: Bool

    func execute(_: std.unittest.TestSuiteReportInfo, tcase: std.unittest.TestCaseResult, configuration: std.unittest.common.Configuration): Unit

    func formatArguments(inputs: std.core.Array<std.unittest.InputParameter>): std.core.Option<std.unittest.common.PrettyText>

    let strategy: std.unittest.DataStrategyProcessor<T>

    let body: (T) -> Unit

    init(strategy: std.unittest.DataStrategyProcessor<T>, body: (T) -> Unit) { /* compiled code */ }
}

extend RunStepResult {
    func withUpdatedStep(newKind: std.unittest.StepKind): std.unittest.RunStepResult
}

func strategyErrorAsResult(e: std.core.Exception, descr: std.unittest.ArgumentDescription): std.unittest.RunStepResult

let SHOW_ALL_OUTPUT_KEY: std.core.String

let NO_CAPTURE_OUTPUT_KEY: std.core.String

let OP_CAPTURE_OUTPUT: std.core.String

let CAPTURE_OUTPUT_KEY: std.core.String

let OP_VERBOSE: std.core.String

let VERBOSE_KEY: std.core.String

func tagsToString(tags: std.core.Array<std.core.String>): std.core.Option<std.core.String>

extend Configuration {
    prop bench: Bool
}

class PackageTextReport {
    var baselineReport: std.collection.HashMap<std.core.String, Float64>

    var baselineId: std.core.String

    var failures: std.collection.HashMap<std.unittest.TestCaseId, Int64>

    func printPackage(packageResult: std.unittest.TestGroupResult): Unit

    let pp: std.unittest.common.PrettyPrinter

    let outputReporter: std.unittest.TestOutputReporter

    let config: std.unittest.common.Configuration

    let detailed: Bool

    init(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration, detailed: Bool) { /* compiled code */ }
}

func appendDashLine(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

class TextReportEntry {
    let checkResults: std.core.Array<std.unittest.CheckResult>

    let duration: std.core.Duration

    let statusCode: std.unittest.TestCode

    let preamble: std.unittest.common.PrettyText

    let caseInfo: std.unittest.TestCaseReportInfo

    let bodyMessage: std.core.Option<std.unittest.common.PrettyText>

    init(source: std.unittest.RunStepResult, caseName: std.core.String, caseInfo: std.unittest.TestCaseReportInfo, bodyMessage: std.core.Option<std.unittest.common.PrettyText>) { /* compiled code */ }
}

class ProjectTextReport {
    func printProject(projectResult: std.unittest.ProjectExecutionResult): Unit

    let pp: std.unittest.common.PrettyPrinter

    let outputReporter: std.unittest.TestOutputReporter

    let config: std.unittest.common.Configuration

    init(pp: std.unittest.common.PrettyPrinter, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration) { /* compiled code */ }
}

class TextReports {
    func printDefaultReport(pp: std.unittest.common.PrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit

    func printProjectSummaryReport(projectResult: std.unittest.ProjectExecutionResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit

    func printIntermediatePackageResult(pp: std.unittest.TerminalPrettyPrinter, packageResult: std.unittest.TestGroupResult, outputReporter: std.unittest.TestOutputReporter, config: std.unittest.common.Configuration): Unit

    init() { /* compiled code */ }
}

func lifecycleStageFailedText(ls: std.unittest.LStep, newLine: Bool): std.unittest.common.PrettyText

func printSummary(pp: std.unittest.common.PrettyPrinter, result: std.unittest.ResultContainer<T>, config: std.unittest.common.Configuration, printFailures: Bool): Unit

extend CheckResult <: PrettyPrintable {
    func pprintWithFailedPrefix(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

    func pprint(pp: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter
}

let OP_TIMEOUT_EACH_NAME: std.core.String

let OP_TIMEOUT_EACH: std.core.String

let TIMEOUT_EACH_KEY: std.core.String

let VALUE_TIMEOUT_PATTERN: std.regex.Regex

func cancellationPoint: Unit

func fillTimeoutKey(configuration: std.unittest.common.Configuration): Unit

class TransportMain <: std.core.Object & std.core.Resource {
    func close: Unit

    func isClosed: Bool

    prop port: UInt16

    func accept: std.unittest.TransportChannel

    func fromDefaultConfiguration: std.unittest.TransportMain

    let socket: std.net.TcpServerSocket

    let outputReporter: std.unittest.TestOutputReporter
}

struct WorkerOutput {
    prop stdouts: std.core.Array<std.core.String>

    prop stderrs: std.core.Array<std.core.String>

    prop captured: std.core.Array<std.unittest.CapturedOutput>

    let stdout: std.core.Array<UInt8>

    let stderr: std.core.Array<UInt8>

    init(stdout: std.core.Array<UInt8>, stderr: std.core.Array<UInt8>) { /* compiled code */ }
}

class UnittestException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class ErrorWrapperException <: std.core.Exception {
    init(error: std.core.Error) { /* compiled code */ }

    init(message: std.core.String, error: std.core.Error) { /* compiled code */ }

    func getClassName: std.core.String

    func toString: std.core.String
}

class UnittestTimeoutException <: std.core.Exception {
    init() { /* compiled code */ }

    func getClassName: std.core.String
}

func CJ_FS_IsFileORLinkToFile(path: CString): Int8

func CJ_FS_IsDirORLinkToFDir(path: CString): Int8

extend DataModel {
    func asStruct: std.unittest.common.DataModelStruct
}

func min(a: T, b: T): T

func max(a: T, b: T): T

func mapParallelOrdered(bufferSize: Int64, f: (T) -> V): (std.core.Iterable<T>) -> std.core.Iterator<V>

class ParallelOrderedMapIterator <: std.core.Iterator<T> {
    var futures: std.collection.ArrayList<std.core.Future<T>>

    var currentIdx: Int64

    func next: std.core.Option<T>

    let bufferSize: Int64

    let iter: std.core.Iterator<std.core.Future<T>>

    init(bufferSize: Int64, iter: std.core.Iterator<std.core.Future<T>>) { /* compiled code */ }
}

class ChainIterator <: std.core.Iterator<T> {
    func next: std.core.Option<T>

    let l: std.core.Iterator<T>

    let r: std.core.Iterator<T>

    init(l: std.core.Iterator<T>, r: std.core.Iterator<T>) { /* compiled code */ }
}

func chain(l: std.core.Iterable<T>, r: std.core.Iterable<T>): std.core.Iterable<T>

func beforeEach(action: (std.core.Option<T>) -> Unit): (std.core.Iterable<T>) -> std.core.Iterator<T>
