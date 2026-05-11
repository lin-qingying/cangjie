// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.mock

import std.collection.ArrayList
import std.core.*
import std.unittest.mock.`internal`.*
import std.unittest.common.*
import std.collection.*
import std.unittest.common.PrettyText
import std.collection.HashMap
import std.sync.*
import std.unittest.mock.`internal`.FuncInfo
import std.unittest.common.PrettyPrintable
import std.unittest.mock.`internal`.ParameterInfo
import std.collection.enumerate
import std.fs.Path

func mock(): T { /* compiled code */ }

enum MockKind {
    | Mock

    | Spy
}

enum StubMode {
    | ReturnsDefaults

    | SyntheticFields
}

class CallHandlerImpl <: std.core.Object & std.unittest.mock.`internal`.CallHandler {
    func onCall(call: std.unittest.mock.`internal`.Call): std.unittest.mock.`internal`.OnCall { /* compiled code */ }

    func requireMockObject(obj: T): T { /* compiled code */ }

    let objId: UInt64

    let objectCreatedAtSessionName: std.core.Option<std.core.String>

    init(objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>) { /* compiled code */ }
}

func create(kind: std.unittest.mock.MockKind, modes: std.core.Array<std.unittest.mock.StubMode>, constructor: (std.unittest.mock.CallHandlerImpl) -> T): T { /* compiled code */ }

func mock(modes: std.core.Array<std.unittest.mock.StubMode>): T { /* compiled code */ }

func spy(objectToSpyOn: T): T { /* compiled code */ }

interface ValueListener {
    func lastValue(): std.core.Option<T> { /* compiled code */ }

    func allValues(): std.core.Array<T> { /* compiled code */ }

    func addCallback(callback: (T) -> Unit): Unit { /* compiled code */ }

    static func new(): std.unittest.mock.ValueListener<T> { /* compiled code */ }

    static func onEach(callback: (T) -> Unit): std.unittest.mock.ValueListener<T> { /* compiled code */ }
}

class ValueListenerImpl <: std.core.Object & std.unittest.mock.ValueListener<T> & std.unittest.mock.ListenerInternal {
    let callBacks: std.collection.ArrayList<(T) -> Unit>

    let values: std.collection.ArrayList<T>

    init(callBack: (T) -> Unit) { /* compiled code */ }

    func lastValue(): std.core.Option<T> { /* compiled code */ }

    func allValues(): std.core.Array<T> { /* compiled code */ }

    func addCallback(callBack: (T) -> Unit): Unit { /* compiled code */ }

    override func supplyValue(value: std.core.Any): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class ActionSelector {
    func mustBeSpy(actionName: std.core.String): Unit { /* compiled code */ }

    func throwsImpl(exception: std.core.Exception): Unit { /* compiled code */ }

    func throwsFactoryImpl(exceptionFactory: () -> std.core.Exception): Unit { /* compiled code */ }

    func returnsImpl(value: TRet): Unit { /* compiled code */ }

    func returnsFactoryImpl(factory: () -> TRet): Unit { /* compiled code */ }

    func returnsCollectionImpl(values: std.core.Collection<TRet>): Unit { /* compiled code */ }

    func fails(): Unit { /* compiled code */ }

    let sc: std.unittest.mock.Scenario

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class Scenario {
    var status: std.unittest.mock.ScenarioStatus

    func setAction(selectedAction: std.unittest.mock.Action): Unit { /* compiled code */ }

    func setCardinality(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    func tryRegisterInvocation(): Bool { /* compiled code */ }

    func activate(): Unit { /* compiled code */ }

    prop invocationCount: Int64 {
        get() { /* compiled code */ }
    }

    prop action: std.unittest.mock.Action {
        get() { /* compiled code */ }
    }

    prop cardinality: std.unittest.mock.Cardinality {
        get() { /* compiled code */ }
    }

    let parentChain: std.unittest.mock.StubChain

    init(parentChain: std.unittest.mock.StubChain) { /* compiled code */ }
}

class MethodActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func continuation(): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    func callsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class CardinalitySelector {
    func anyTimes(): Unit { /* compiled code */ }

    func once(): std.unittest.mock.Continuation<A> { /* compiled code */ }

    func atLeastOnce(): Unit { /* compiled code */ }

    func times(expectedTimes: Int64): std.unittest.mock.Continuation<A> { /* compiled code */ }

    func times(min: Int64, max: Int64): Unit { /* compiled code */ }

    func atLeastTimes(minTimesExpected: Int64): Unit { /* compiled code */ }

    let sc: std.unittest.mock.Scenario

    let factory: (std.unittest.mock.Scenario) -> A

    init(sc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

class Continuation {
    func then(): A { /* compiled code */ }

    let previousSc: std.unittest.mock.Scenario

    let factory: (std.unittest.mock.Scenario) -> A

    init(previousSc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

class GetterActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func continuation(): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func getsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    func getsField(field: std.unittest.mock.SyntheticField<TRet>): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class SyntheticField {
    static func create(initialValue: T): std.unittest.mock.SyntheticField<T> { /* compiled code */ }

    let description: std.unittest.mock.SyntheticFieldDescription

    init(description: std.unittest.mock.SyntheticFieldDescription) { /* compiled code */ }
}

class SetterActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    func doesNothing(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    func setsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    func setsField(field: std.unittest.mock.SyntheticField<TArg>): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

extend MethodActionSelector {
    func returns(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<Unit>> { /* compiled code */ }
}

enum StubMisconfigurationReason {
    | ActionNotSpecified

    | CardinalityAlreadySet

    | ActionAlreadySet

    | CannotModifyActive

    | OnlySpyCanCallOriginal(std.core.String)
}

class StubMisconfiguration <: std.unittest.mock.FailureReport {
    func fail(stub: std.unittest.mock.StubChain, reason: std.unittest.mock.StubMisconfigurationReason): Nothing { /* compiled code */ }

    func stubConfigurationHint(reason: std.unittest.mock.StubMisconfigurationReason): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class StubChain {
    var wasActivated: Bool

    let scenarios: std.collection.ArrayList<std.unittest.mock.Scenario>

    func addScenario(_: std.unittest.mock.MockSession): std.unittest.mock.Scenario { /* compiled code */ }

    func ensureActivated(): Unit { /* compiled code */ }

    func isVerifiable(): Bool { /* compiled code */ }

    let invocationMatcher: std.unittest.mock.InvocationMatcher

    let objectKind: std.unittest.mock.MockKind

    let parentSession: std.unittest.mock.MockSession

    init(invocationMatcher: std.unittest.mock.InvocationMatcher, objectKind: std.unittest.mock.MockKind, parentSession: std.unittest.mock.MockSession) { /* compiled code */ }
}

class FailureReport {
    let textBuilder: std.unittest.common.PrettyText

    var lastSymbol: Rune

    init() { /* compiled code */ }

    func build(body: () -> Unit): std.unittest.common.PrettyText { /* compiled code */ }

    func renderStatement(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

    func renderStub(stubChain: std.unittest.mock.StubChain): Unit { /* compiled code */ }

    func renderMatcher(invocationMatcher: std.unittest.mock.InvocationMatcher, atString: std.core.String, prefixString: std.core.String): Unit { /* compiled code */ }

    func renderStubDescription(description: std.unittest.mock.StubDescription, atString: std.core.String, prefixString: std.core.String): Unit { /* compiled code */ }

    func renderMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Any { /* compiled code */ }

    func findMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.collection.ArrayList<std.unittest.mock.Invocation> { /* compiled code */ }

    func renderInvocationList(invocations: std.core.Collection<std.unittest.mock.Invocation>): Unit { /* compiled code */ }

    func renderInvocationWithLocation(invocation: std.unittest.mock.Invocation, atString: std.core.String): Unit { /* compiled code */ }

    func renderInvocation(invocation: std.unittest.mock.Invocation): Unit { /* compiled code */ }

    func renderLocation(location: std.unittest.mock.Location): Unit { /* compiled code */ }

    func renderDeclarationNameAndKind(info: std.unittest.mock.`internal`.FuncInfo, getterSetterPrefix: Bool): Unit { /* compiled code */ }

    func reportArgumentMismatch(invocationMatcher: std.unittest.mock.InvocationMatcher, unmatchedArgumentIndices: std.core.Array<Int64>): Unit { /* compiled code */ }

    func renderCardinalityMin(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    func renderCardinalityMax(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    func renderRequiredActual(actualInvocations: Int64, renderCardinality: () -> Unit): Unit { /* compiled code */ }

    func timesStr(i: Int64): Unit { /* compiled code */ }

    func indent(body: () -> Unit): Unit { /* compiled code */ }

    func line(str: std.core.String): Unit { /* compiled code */ }

    func line(body: () -> Unit): Unit { /* compiled code */ }

    func listEntry(body: () -> Unit): Unit { /* compiled code */ }

    func nl(ensureDotAtTheEnd: Bool): Unit { /* compiled code */ }

    func text(str: std.core.String): Unit { /* compiled code */ }

    func append(str: std.core.String): Unit { /* compiled code */ }

    func userCode(userCode: std.core.ToString): Unit { /* compiled code */ }

    func error(errorText: std.core.ToString): Unit { /* compiled code */ }

    func success(successText: std.core.ToString): Unit { /* compiled code */ }

    func errorLine(text: std.core.String): Unit { /* compiled code */ }

    func errorHeader(text: std.core.String): Unit { /* compiled code */ }

    func includeContent(content: std.unittest.common.PrettyPrintable): Unit { /* compiled code */ }

    let invocationLog: std.unittest.mock.LogSnapshot

    let nameRegistry: std.unittest.mock.MockNameRegistry

    init(invocationLog: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class StubModeViolation <: std.unittest.mock.FailureReport {
    func stubForbidden(stubDescription: std.unittest.mock.StubDescription, sessionName: std.core.String): Nothing { /* compiled code */ }

    func statefulStubAction(actionName: std.core.String, stubChain: std.unittest.mock.StubChain): Nothing { /* compiled code */ }

    func cardinalitySpecifierNotAllowed(stubChain: std.unittest.mock.StubChain): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

struct StubDescription {
    let fullCall: std.core.String

    let prefixRefName: std.core.Option<std.core.String>

    let location: std.unittest.mock.Location

    init(fullCall: std.core.String, prefixRefName: std.core.Option<std.core.String>, location: std.unittest.mock.Location) { /* compiled code */ }
}

enum VerifyMisconfigurationReason {
    | StatementUsedTwice(std.unittest.mock.VerifyStatement)

    | StatementNotUsed(std.unittest.mock.VerifyStatement)

    | ArgumentCaptureNotAllowed(std.unittest.mock.VerifyStatement, std.unittest.mock.ArgumentMatcher)

    | CardinalityCannotBeReassigned(std.unittest.mock.VerifyStatement)

    | EmptyVerifyBlock
}

class VerifyStatement {
    var _cardinality: std.core.Option<std.unittest.mock.Cardinality>

    var isUsed: Bool

    prop cardinality: std.unittest.mock.Cardinality {
        get() { /* compiled code */ }
    }

    static func fromStub(stubCall: () -> R, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, objName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func prepareForVerification(orderedness: std.unittest.mock.Orderedness): Unit { /* compiled code */ }

    func setCardinality(newCardinality: std.unittest.mock.Cardinality): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func once(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func atLeastOnce(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func times(expectedTimes: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func times(min: Int64, max: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func atLeastTimes(minTimesExpected: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    func never(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    let invocationMatcher: std.unittest.mock.InvocationMatcher

    init(invocationMatcher: std.unittest.mock.InvocationMatcher) { /* compiled code */ }
}

class ArgumentMatcher {
    var description: std.core.Option<std.core.String>

    var parameterName: std.core.Option<std.core.String>

    func getDescription(): std.core.Option<std.core.String> { /* compiled code */ }

    func withDescription(description: std.core.String): std.unittest.mock.ArgumentMatcher { /* compiled code */ }

    func getParameterName(): std.core.Option<std.core.String> { /* compiled code */ }

    func forParameter(name: std.core.String): std.unittest.mock.ArgumentMatcher { /* compiled code */ }

    func matchesAny(arg: std.core.Any): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

class VerifyMisconfiguration <: std.unittest.mock.FailureReport {
    func fail(reason: std.unittest.mock.VerifyMisconfigurationReason): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

interface ListeningMatcher {
    prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal> {
        get() { /* compiled code */ }
    }
}

interface ListenerInternal {
    func supplyValue(value: std.core.Any): Unit { /* compiled code */ }
}

class TypedMatcher <: std.unittest.mock.ArgumentMatcher {
    func matches(arg: T): Bool { /* compiled code */ }

    override func matchesAny(arg: std.core.Any): Bool { /* compiled code */ }

    let expectedBooleanValue: Bool

    init(expectedBooleanValue: Bool) { /* compiled code */ }
}

class PredicateMatcher <: std.unittest.mock.TypedMatcher<T> & std.unittest.mock.ListeningMatcher {
    override func matches(arg: T): Bool { /* compiled code */ }

    prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal> {
        get() { /* compiled code */ }
    }

    let predicate: (T) -> Bool

    let expectedBooleanValue: Bool

    let listener: std.core.Option<std.unittest.mock.ListenerInternal>

    init(predicate: (T) -> Bool, expectedBooleanValue: Bool, listener: std.core.Option<std.unittest.mock.ListenerInternal>) { /* compiled code */ }
}

class AnyMatcher <: std.unittest.mock.ArgumentMatcher {
    func matchesAny(_: std.core.Any): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

class Matchers {
    static func argThat(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func argThatNot(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func argThat(listener: std.unittest.mock.ValueListener<T>, predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func same(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func eq(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func ofType(): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func any(): std.unittest.mock.AnyMatcher { /* compiled code */ }

    static func capture(listener: std.unittest.mock.ValueListener<T>): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    static func default(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }
}

extend TypedMatcher {
    func value(): T { /* compiled code */ }
}

extend AnyMatcher {
    func value(): T { /* compiled code */ }
}

class Cardinality {
    func check(timesInvoked: Int64): std.unittest.mock.CardinalityCheck { /* compiled code */ }

    static func once(): std.unittest.mock.Cardinality { /* compiled code */ }

    static func atLeastTimes(minTimes: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    static func atLeastOnce(): std.unittest.mock.Cardinality { /* compiled code */ }

    static func times(min: Int64, max: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    static func times(timesExactly: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    static func never(): std.unittest.mock.Cardinality { /* compiled code */ }

    let min: Int64

    let max: std.core.Option<Int64>

    init(min: Int64, max: std.core.Option<Int64>) { /* compiled code */ }
}

enum CardinalityCheck {
    | Less

    | Ok

    | More
}

func require(condition: Bool, message: () -> std.core.String): Unit { /* compiled code */ }

class ConfigureMock {
    static func stubFunction(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, methodName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.MethodActionSelector<TRet> { /* compiled code */ }

    static func stubGetter(stubCall: () -> TRet, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.GetterActionSelector<TRet> { /* compiled code */ }

    static func stubSetter(stubCall: () -> Unit, _: () -> TArg, matcher: std.unittest.mock.ArgumentMatcher, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.SetterActionSelector<TArg> { /* compiled code */ }

    static func createScenario(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.Scenario { /* compiled code */ }
}

func requireObject(ref: std.core.Any, name: std.core.String): std.core.Object { /* compiled code */ }

class PrettyException <: std.core.Exception & std.unittest.common.PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    let text: std.unittest.common.PrettyText

    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class MockFrameworkInternalError <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class MockFrameworkException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class ExpectationFailedException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class UnnecessaryStubbingException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class UnstubbedInvocationException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class UnhandledCallException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

class VerificationFailedException <: std.unittest.mock.PrettyException {
    init(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

extend MockSession {
    func verifyExpectations(): Unit { /* compiled code */ }

    func checkForMisconfigurationIssues(): Unit { /* compiled code */ }
}

class MockSession {
    let invocationLog: std.unittest.mock.MutableLog

    let chainsByStubId: std.collection.HashMap<std.unittest.mock.StubId, std.collection.ArrayList<std.unittest.mock.StubChain>>

    let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    let objectRegistry: std.collection.HashMap<UInt64, std.unittest.mock.MockObject>

    let fieldStorage: std.unittest.mock.FieldStorage

    let name: std.core.String

    let sessionKind: std.unittest.mock.MockSessionKind

    init(name: std.core.String, sessionKind: std.unittest.mock.MockSessionKind) { /* compiled code */ }
}

extend StubChain {
    func verifyExpectations(): std.core.Option<std.unittest.mock.ExpectationFailedReason> { /* compiled code */ }

    func getTotalCardinality(): std.unittest.mock.Cardinality { /* compiled code */ }

    func getTotalInvocationCount(): Int64 { /* compiled code */ }
}

enum ExpectationFailedReason {
    | TooFewInvocationsForStubChain(std.unittest.mock.StubChain, std.unittest.mock.Cardinality, Int64)
}

class ExpectationFailedReport <: std.unittest.mock.FailureReport {
    static func fail(log: std.unittest.mock.LogSnapshot, failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): Nothing { /* compiled code */ }

    func renderExpectationFailures(failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): std.unittest.common.PrettyText { /* compiled code */ }

    func renderExpectationFailedReason(reason: std.unittest.mock.ExpectationFailedReason): Unit { /* compiled code */ }

    func renderInvocationListWithHandledByHints(entries: std.core.Array<std.unittest.mock.LogEntry>): Unit { /* compiled code */ }

    func findEntriesByStubId(stubId: std.unittest.mock.StubId): std.collection.ArrayList<std.unittest.mock.LogEntry> { /* compiled code */ }

    init(log: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class LogSnapshot {
    func invocations(): std.core.Array<std.unittest.mock.Invocation> { /* compiled code */ }

    let EMPTY: std.unittest.mock.LogSnapshot

    let entries: std.core.Array<std.unittest.mock.LogEntry>

    init(entries: std.core.Array<std.unittest.mock.LogEntry>) { /* compiled code */ }
}

struct LogEntry {
    func wasHandledBy(thatChain: std.unittest.mock.StubChain): Bool { /* compiled code */ }

    let invocation: std.unittest.mock.Invocation

    let origin: std.unittest.mock.BehaviourSource

    init(invocation: std.unittest.mock.Invocation, origin: std.unittest.mock.BehaviourSource) { /* compiled code */ }
}

class StubId <: std.core.Object & std.core.Hashable & std.core.Equatable<std.unittest.mock.StubId> {
    func hashCode(): Int64 { /* compiled code */ }

    operator func ==(that: std.unittest.mock.StubId): Bool { /* compiled code */ }

    operator func !=(that: std.unittest.mock.StubId): Bool { /* compiled code */ }

    let funcInfo: std.unittest.mock.`internal`.FuncInfo

    let receiverId: UInt64

    init(funcInfo: std.unittest.mock.`internal`.FuncInfo, receiverId: UInt64) { /* compiled code */ }
}

class MockNameRegistry {
    let idToName: std.collection.HashMap<UInt64, std.core.Option<std.core.String>>

    func submit(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Option<std.core.Option<std.core.String>> { /* compiled code */ }

    func `get`(objId: UInt64): std.core.Option<std.core.String> { /* compiled code */ }

    let EMPTY: std.unittest.mock.MockNameRegistry

    init(matchers: std.core.Array<std.unittest.mock.InvocationMatcher>) { /* compiled code */ }
}

class InvocationMatcher {
    let positionalMatchers: std.collection.ArrayList<std.unittest.mock.ArgumentMatcher>

    func tryMatch(invocation: std.unittest.mock.Invocation): std.unittest.mock.MatchStatus { /* compiled code */ }

    func matches(invocation: std.unittest.mock.Invocation): Bool { /* compiled code */ }

    let typeParameters: std.core.Array<std.core.String>

    let stubId: std.unittest.mock.StubId

    let description: std.unittest.mock.StubDescription

    init(argumentMatchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, typeParameters: std.core.Array<std.core.String>, stubId: std.unittest.mock.StubId, description: std.unittest.mock.StubDescription) { /* compiled code */ }
}

class Invocation {
    let stubId: std.unittest.mock.StubId

    let mockObject: std.core.Option<std.unittest.mock.MockObject>

    let call: std.unittest.mock.`internal`.Call

    let location: std.unittest.mock.Location

    init(mockObject: std.core.Option<std.unittest.mock.MockObject>, call: std.unittest.mock.`internal`.Call, location: std.unittest.mock.Location) { /* compiled code */ }
}

class Location {
    func getEntries(): std.core.Array<std.unittest.mock.TraceEntry> { /* compiled code */ }

    init() { /* compiled code */ }
}

class SyntheticFieldDescription {
    let id: UInt64

    let initialValue: std.core.Any

    init(initialValue: std.core.Any) { /* compiled code */ }
}

class FieldStorage {
    let explicitFields: std.collection.HashMap<UInt64, std.core.Box<std.core.Any>>

    let autoFields: std.collection.HashMap<std.unittest.mock.AutoFieldId, std.core.Box<std.core.Any>>

    func getExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription): std.core.Any { /* compiled code */ }

    func setExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription, value: std.core.Any): Unit { /* compiled code */ }

    func getAutoFieldValue(id: std.unittest.mock.AutoFieldId): std.core.Option<std.core.Any> { /* compiled code */ }

    func setAutoFieldValue(id: std.unittest.mock.AutoFieldId, value: std.core.Any): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

struct AutoFieldId <: std.core.Hashable & std.core.Equatable<std.unittest.mock.AutoFieldId> {
    init(mockObject: std.unittest.mock.MockObject, memberName: std.core.String) { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    operator func ==(that: std.unittest.mock.AutoFieldId): Bool { /* compiled code */ }

    operator func !=(that: std.unittest.mock.AutoFieldId): Bool { /* compiled code */ }

    let mockObjectId: UInt64

    let memberName: std.core.String

    init(mockObjectId: UInt64, memberName: std.core.String) { /* compiled code */ }
}

class MockObject {
    let id: UInt64

    let ref: std.core.Object

    let kind: std.unittest.mock.MockKind

    let modes: std.core.Array<std.unittest.mock.StubMode>

    let sessionName: std.core.Option<std.core.String>

    init(id: UInt64, ref: std.core.Object, kind: std.unittest.mock.MockKind, modes: std.core.Array<std.unittest.mock.StubMode>, sessionName: std.core.Option<std.core.String>) { /* compiled code */ }
}

class FieldsErrorReport <: std.unittest.mock.FailureReport {
    func errorReadingField(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    func valueNotSet(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    func mustUseStubs(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    init() { /* compiled code */ }
}

let _FRAMEWORK: std.unittest.mock.MockFramework

class MockFramework {
    let sessions: std.collection.ArrayList<std.unittest.mock.MockSession>

    var recording: std.core.Option<std.unittest.mock.RecordingHandler>

    let globalLock: std.sync.ReentrantMutex

    let objCounter: std.sync.AtomicUInt64

    init() { /* compiled code */ }

    static func openSession(name: std.core.String, sessionKind: std.unittest.mock.MockSessionKind): Unit { /* compiled code */ }

    static func closeSession(): Unit { /* compiled code */ }

    func generateUniqueId(): UInt64 { /* compiled code */ }

    static func session(body: (std.unittest.mock.MockSession) -> T): T { /* compiled code */ }

    static func recordCall(recordingInfo: std.unittest.mock.RecordingInfo, dummyCall: () -> R): std.unittest.mock.RecordedCall { /* compiled code */ }

    func handleSync(call: std.unittest.mock.`internal`.Call, objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>): std.unittest.mock.`internal`.OnCall { /* compiled code */ }

    func registerObject(mockObject: std.unittest.mock.MockObject): Unit { /* compiled code */ }

    func findObjectIdsByRefs(referencesToFind: std.core.Array<std.core.Object>): std.collection.Set<UInt64> { /* compiled code */ }

    func throwNotAMockObject(): Nothing { /* compiled code */ }
}

type UnmatchedChains = std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>

enum MatchStatus {
    | Ok

    | FuncMismatch

    | ReceiverMismatch

    | ArgumentMismatch(std.core.Array<Int64>)

    | TypeArgMismatch(std.core.Array<Int64>)
}

class RecordingHandler {
    var data: std.core.Option<std.unittest.mock.RecordedCall>

    func record(call: std.unittest.mock.`internal`.Call, object: std.core.Option<std.unittest.mock.MockObject>): Unit { /* compiled code */ }

    func getRecorded(): std.unittest.mock.RecordedCall { /* compiled code */ }

    let info: std.unittest.mock.RecordingInfo

    init(info: std.unittest.mock.RecordingInfo) { /* compiled code */ }
}

enum MockSessionKind {
    | Forbidden

    | Stateless

    | Verifiable
}

struct RecordingInfo {
    let target: std.unittest.mock.RecordingTarget

    let prefixRefName: std.core.Option<std.core.String>

    let declarationName: std.core.String

    init(target: std.unittest.mock.RecordingTarget, prefixRefName: std.core.Option<std.core.String>, declarationName: std.core.String) { /* compiled code */ }
}

struct RecordedCall {
    let object: std.core.Option<std.unittest.mock.MockObject>

    let stubId: std.unittest.mock.StubId

    let location: std.unittest.mock.Location

    let typeArgs: std.core.Array<std.core.String>

    init(object: std.core.Option<std.unittest.mock.MockObject>, stubId: std.unittest.mock.StubId, location: std.unittest.mock.Location, typeArgs: std.core.Array<std.core.String>) { /* compiled code */ }
}

extend MockObject {
    func hasMode(mode: std.unittest.mock.StubMode): Bool { /* compiled code */ }
}

extend Invocation {
    prop isDefaultEnabled: Bool {
        get() { /* compiled code */ }
    }

    prop canReturnDefaultValue: Bool {
        get() { /* compiled code */ }
    }
}

class MockScopeViolation <: std.unittest.mock.FailureReport {
    let HINT: std.core.String

    func reportInaccessible(createdAtSessionName: std.core.Option<std.core.String>, whereSessionName: std.core.Option<std.core.String>): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

enum SessionHandleResult {
    | PerformScenario(std.unittest.mock.Scenario)

    | Unhandled

    | InvocationLimitExceeded(std.unittest.mock.StubChain)
}

extend MockSession {
    func `handle`(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>): std.unittest.mock.SessionHandleResult { /* compiled code */ }
}

extend StubChain {
    func runListeners(invocation: std.unittest.mock.Invocation): Unit { /* compiled code */ }
}

enum InternalError {
    | InconsistentState(std.core.String)

    | AssertionFailed(std.core.String)

    | Unimplemented(std.core.String)
}

func internalError(e: std.unittest.mock.InternalError): Nothing { /* compiled code */ }

func internalError(assertion: std.core.String): Nothing { /* compiled code */ }

func illegalInput(text: std.core.String): Nothing { /* compiled code */ }

enum BehaviourSource {
    | Stub(std.unittest.mock.StubChain)

    | Default

    | Failure(std.unittest.mock.PrettyException)
}

class MutableLog {
    let entries: std.collection.ArrayList<std.unittest.mock.LogEntry>

    var lastClearIndex: Int64

    func newEntry(entry: std.unittest.mock.LogEntry): Unit { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func snapshotSinceLastClear(scope: std.collection.Set<UInt64>): std.unittest.mock.LogSnapshot { /* compiled code */ }

    func fullLog(): std.unittest.mock.LogSnapshot { /* compiled code */ }

    func validateLog(): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class LogCorruptedReport <: std.unittest.mock.FailureReport {
    func fail(originalMessage: std.unittest.common.PrettyPrintable): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

struct TraceEntry {
    let filePath: std.core.String

    let lineNumber: Int64

    init(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let UNKNOWN_LOCATION: std.unittest.mock.LocationImpl

class LocationImpl <: std.unittest.mock.Location {
    func getEntries(): std.core.Array<std.unittest.mock.TraceEntry> { /* compiled code */ }

    let entries: std.core.Array<std.unittest.mock.TraceEntry>

    init(entries: std.core.Array<std.unittest.mock.TraceEntry>) { /* compiled code */ }
}

let UNKNOWN_LOCATION_STRING: std.core.String

class SimpleLocation <: std.unittest.mock.Location {
    func getEntries(): std.core.Array<std.unittest.mock.TraceEntry> { /* compiled code */ }

    let filePath: std.core.String

    let lineNumber: Int64

    init(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let MOCKED_CLASS_NAME_FRAGMENT: std.core.String

func locationFromTrace(): std.unittest.mock.Location { /* compiled code */ }

extend Location {
    func forceLineNumber(lineNum: Int64): std.unittest.mock.Location { /* compiled code */ }

    func fileNameAndLine(): std.core.String { /* compiled code */ }
}

class NoneMatcher <: std.unittest.mock.ArgumentMatcher {
    override func matchesAny(arg: std.core.Any): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

extend Matchers {
    static func none(): std.unittest.mock.NoneMatcher { /* compiled code */ }
}

extend NoneMatcher {
    func value(): std.core.Option<T> { /* compiled code */ }
}

enum RecordingTarget {
    | MethodStub

    | VerifyStatementStub
}

enum RecordingFailureReason {
    | NotAMock

    | UnmockableCallable
}

class RecordingFailureReport <: std.unittest.mock.FailureReport {
    func fail(info: std.unittest.mock.RecordingInfo, possibleReasons: std.core.Array<std.unittest.mock.RecordingFailureReason>): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

enum RuntimeFailureReason {
    | UnstubbedDeclaration(std.unittest.mock.`internal`.FuncInfo)

    | NoneMatchedArguments(std.core.Array<(std.unittest.mock.StubChain, std.core.Array<Int64>)>)

    | TooManyInvocationsForStubChain(std.unittest.mock.StubChain)

    | UnwantedInteraction(std.unittest.mock.StubChain)
}

func unhandledInvocationReport(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

func invocationLimitExceededReport(currentInvocation: std.unittest.mock.Invocation, chain: std.unittest.mock.StubChain, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

func runtimeUnwantedInteraction(currentInvocation: std.unittest.mock.Invocation, sc: std.unittest.mock.Scenario, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

class RuntimeFailureReport <: std.unittest.mock.FailureReport {
    func render(invocation: std.unittest.mock.Invocation, reason: std.unittest.mock.RuntimeFailureReason): std.unittest.common.PrettyText { /* compiled code */ }

    func findInvocationsHandledByChain(stubChain: std.unittest.mock.StubChain): std.core.Array<std.unittest.mock.Invocation> { /* compiled code */ }

    init(invocationLog: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

enum Action {
    | Return(() -> std.core.Any)

    | Throw(() -> std.core.Exception)

    | CallOriginal

    | Fail

    | GetField(std.unittest.mock.SyntheticFieldDescription)

    | SetField(std.unittest.mock.SyntheticFieldDescription)
}

struct ScenarioConfiguration {
    let action: std.unittest.mock.Action

    let cardinality: std.unittest.mock.Cardinality

    init(action: std.unittest.mock.Action, cardinality: std.unittest.mock.Cardinality) { /* compiled code */ }
}

enum ScenarioStatus {
    | Created(std.core.Option<std.unittest.mock.Action>, std.core.Option<std.unittest.mock.Cardinality>)

    | Active(std.unittest.mock.ScenarioConfiguration, std.core.Box<Int64>)
}

interface UntypedOption {
    func value(): std.core.Option<std.core.Any> { /* compiled code */ }
}

extend Option <: UntypedOption {
    func value(): std.core.Option<std.core.Any> { /* compiled code */ }
}

func unwrapValue(value: std.core.Any): std.core.Option<T> { /* compiled code */ }

enum Exhaustiveness {
    | Exhaustive

    | Partial
}

enum Orderedness {
    | Ordered

    | Unordered
}

enum VerificationResult {
    | Success

    | Failure(std.unittest.mock.VerificationFailedReason)
}

enum VerificationFailedReason {
    | TooFewInvocations(std.unittest.mock.VerifyStatement, std.unittest.mock.Cardinality, Int64)

    | TooManyInvocations(std.unittest.mock.VerifyStatement, std.unittest.mock.Cardinality, Int64)

    | UnmatchedStatements(std.core.Array<std.unittest.mock.VerifyStatement>)

    | UnmatchedInvocations(std.core.Array<std.unittest.mock.Invocation>)

    | UnexpectedInvocation(std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)

    | NonDisjointStatements(std.unittest.mock.VerifyStatement, std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)

    | UnwantedInteractions(std.core.Array<std.unittest.mock.Invocation>)
}

func performVerification(statements: std.core.Array<std.unittest.mock.VerifyStatement>, orderedness: std.unittest.mock.Orderedness, exhaustiveness: std.unittest.mock.Exhaustiveness): Unit { /* compiled code */ }

func verifyNoInteractions(objectRefs: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

func ensureDistinctReferences(refs: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

func collectObjectsIds(statements: std.core.Array<std.unittest.mock.VerifyStatement>): std.collection.HashSet<UInt64> { /* compiled code */ }

func checkMatchersConsistency(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

class VerificationFailureReport <: std.unittest.mock.FailureReport {
    func fail(reason: std.unittest.mock.VerificationFailedReason): Nothing { /* compiled code */ }

    init(logSnapshot: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class Verify {
    static func that(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

    static func unordered(exhaustive: std.unittest.mock.Exhaustiveness, collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit { /* compiled code */ }

    static func unordered(collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit { /* compiled code */ }

    static func unordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    static func unordered(exhaustive: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    static func ordered(collectStatements: (std.unittest.mock.OrderedVerifier) -> Unit): Unit { /* compiled code */ }

    static func ordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    static func clearInvocationLog(): Unit { /* compiled code */ }

    static func noInteractions(mockObjects: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

class UnorderedVerifier {
    let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.UnorderedVerifier { /* compiled code */ }

    init() { /* compiled code */ }
}

class OrderedVerifier {
    let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.OrderedVerifier { /* compiled code */ }

    init() { /* compiled code */ }
}

class VerifyIterator {
    let statementIterator: std.core.Iterator<std.unittest.mock.VerifyStatement>

    var currentStatement: std.unittest.mock.VerifyStatement

    var nextStatement: std.core.Option<std.unittest.mock.VerifyStatement>

    var logIndex: Int64

    var invocation: std.unittest.mock.Invocation

    var currentStatementMatch: Bool

    var nextStatementMatch: Bool

    func goToNextStatement(): Bool { /* compiled code */ }

    func hasMoreStatements(): Bool { /* compiled code */ }

    func goToNextInvocation(): Bool { /* compiled code */ }

    func check(count: Int64): std.unittest.mock.CardinalityCheck { /* compiled code */ }

    let statements: std.core.Iterable<std.unittest.mock.VerifyStatement>

    let invocations: std.core.Array<std.unittest.mock.Invocation>

    init(statements: std.core.Iterable<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>) { /* compiled code */ }
}

func verifyExhaustiveOrderedIteration(iterator: std.unittest.mock.VerifyIterator): std.core.Option<std.unittest.mock.VerificationResult> { /* compiled code */ }

func verifyExhaustiveOrdered(statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult { /* compiled code */ }

extend VerifyIterator {
    func tooFewInvocations(count: Int64): std.unittest.mock.VerificationResult { /* compiled code */ }

    func tooManyInvocations(count: Int64): std.unittest.mock.VerificationResult { /* compiled code */ }

    func nonDisjointStatements(): std.unittest.mock.VerificationResult { /* compiled code */ }

    func unmatchedStatements(): std.unittest.mock.VerificationResult { /* compiled code */ }

    func unmatchedInvocations(): std.unittest.mock.VerificationResult { /* compiled code */ }

    func unexpectedInvocation(): std.unittest.mock.VerificationResult { /* compiled code */ }

    func leftoverStatements(): std.core.Array<std.unittest.mock.VerifyStatement> { /* compiled code */ }
}

func verifyUnordered(exhastiveness: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult { /* compiled code */ }

func batchMatch(statements: std.core.Array<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>): std.unittest.mock.BatchMatchResult { /* compiled code */ }

enum BatchMatchResult {
    | DisjointMatching(std.core.Array<std.core.Option<std.unittest.mock.VerifyStatement>>, std.core.Array<Int64>)

    | Ambiguity(std.unittest.mock.VerifyStatement, std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)
}
