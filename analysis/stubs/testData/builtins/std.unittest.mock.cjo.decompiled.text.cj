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

func mock: T

enum MockKind {
    | Mock

    | Spy
}

enum StubMode {
    | ReturnsDefaults

    | SyntheticFields
}

class CallHandlerImpl <: std.core.Object & std.unittest.mock.`internal`.CallHandler {
    func onCall(call: std.unittest.mock.`internal`.Call): std.unittest.mock.`internal`.OnCall

    func requireMockObject(obj: T): T

    let objId: UInt64

    let objectCreatedAtSessionName: std.core.Option<std.core.String>

    init(objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>) { /* compiled code */ }
}

func create(kind: std.unittest.mock.MockKind, modes: std.core.Array<std.unittest.mock.StubMode>, constructor: (std.unittest.mock.CallHandlerImpl) -> T): T

func mock(modes: std.core.Array<std.unittest.mock.StubMode>): T

func spy(objectToSpyOn: T): T

interface ValueListener {
    func lastValue: std.core.Option<T>

    func allValues: std.core.Array<T>

    func addCallback(callback: (T) -> Unit): Unit

    func new: std.unittest.mock.ValueListener<T>

    func onEach(callback: (T) -> Unit): std.unittest.mock.ValueListener<T>
}

class ValueListenerImpl <: std.core.Object & std.unittest.mock.ValueListener<T> & std.unittest.mock.ListenerInternal {
    let callBacks: std.collection.ArrayList<(T) -> Unit>

    let values: std.collection.ArrayList<T>

    init(callBack: (T) -> Unit) { /* compiled code */ }

    func lastValue: std.core.Option<T>

    func allValues: std.core.Array<T>

    func addCallback(callBack: (T) -> Unit): Unit

    func supplyValue(value: std.core.Any): Unit

    init() { /* compiled code */ }
}

class ActionSelector {
    func mustBeSpy(actionName: std.core.String): Unit

    func throwsImpl(exception: std.core.Exception): Unit

    func throwsFactoryImpl(exceptionFactory: () -> std.core.Exception): Unit

    func returnsImpl(value: TRet): Unit

    func returnsFactoryImpl(factory: () -> TRet): Unit

    func returnsCollectionImpl(values: std.core.Collection<TRet>): Unit

    func fails: Unit

    let sc: std.unittest.mock.Scenario

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class Scenario {
    var status: std.unittest.mock.ScenarioStatus

    func setAction(selectedAction: std.unittest.mock.Action): Unit

    func setCardinality(cardinality: std.unittest.mock.Cardinality): Unit

    func tryRegisterInvocation: Bool

    func activate: Unit

    prop invocationCount: Int64

    prop action: std.unittest.mock.Action

    prop cardinality: std.unittest.mock.Cardinality

    let parentChain: std.unittest.mock.StubChain

    init(parentChain: std.unittest.mock.StubChain) { /* compiled code */ }
}

class MethodActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector: std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    func continuation: std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>>

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>>

    func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>>

    func callsOriginal: std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>>

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class CardinalitySelector {
    func anyTimes: Unit

    func once: std.unittest.mock.Continuation<A>

    func atLeastOnce: Unit

    func times(expectedTimes: Int64): std.unittest.mock.Continuation<A>

    func times(min: Int64, max: Int64): Unit

    func atLeastTimes(minTimesExpected: Int64): Unit

    let sc: std.unittest.mock.Scenario

    let factory: (std.unittest.mock.Scenario) -> A

    init(sc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

class Continuation {
    func then: A

    let previousSc: std.unittest.mock.Scenario

    let factory: (std.unittest.mock.Scenario) -> A

    init(previousSc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

class GetterActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector: std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func continuation: std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>>

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>>

    func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>>

    func getsOriginal: std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    func getsField(field: std.unittest.mock.SyntheticField<TRet>): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>>

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

class SyntheticField {
    func create(initialValue: T): std.unittest.mock.SyntheticField<T>

    let description: std.unittest.mock.SyntheticFieldDescription

    init(description: std.unittest.mock.SyntheticFieldDescription) { /* compiled code */ }
}

class SetterActionSelector <: std.unittest.mock.ActionSelector {
    func cardinalitySelector: std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    func doesNothing: std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    func setsOriginal: std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    func setsField(field: std.unittest.mock.SyntheticField<TArg>): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>>

    init(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

extend MethodActionSelector {
    func returns: std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<Unit>>
}

enum StubMisconfigurationReason {
    | ActionNotSpecified

    | CardinalityAlreadySet

    | ActionAlreadySet

    | CannotModifyActive

    | OnlySpyCanCallOriginal(std.core.String)
}

class StubMisconfiguration <: std.unittest.mock.FailureReport {
    func fail(stub: std.unittest.mock.StubChain, reason: std.unittest.mock.StubMisconfigurationReason): Nothing

    func stubConfigurationHint(reason: std.unittest.mock.StubMisconfigurationReason): Unit

    init() { /* compiled code */ }
}

class StubChain {
    var wasActivated: Bool

    let scenarios: std.collection.ArrayList<std.unittest.mock.Scenario>

    func addScenario(_: std.unittest.mock.MockSession): std.unittest.mock.Scenario

    func ensureActivated: Unit

    func isVerifiable: Bool

    let invocationMatcher: std.unittest.mock.InvocationMatcher

    let objectKind: std.unittest.mock.MockKind

    let parentSession: std.unittest.mock.MockSession

    init(invocationMatcher: std.unittest.mock.InvocationMatcher, objectKind: std.unittest.mock.MockKind, parentSession: std.unittest.mock.MockSession) { /* compiled code */ }
}

class FailureReport {
    let textBuilder: std.unittest.common.PrettyText

    var lastSymbol: Rune

    init() { /* compiled code */ }

    func build(body: () -> Unit): std.unittest.common.PrettyText

    func renderStatement(statement: std.unittest.mock.VerifyStatement): Unit

    func renderStub(stubChain: std.unittest.mock.StubChain): Unit

    func renderMatcher(invocationMatcher: std.unittest.mock.InvocationMatcher, atString: std.core.String, prefixString: std.core.String): Unit

    func renderStubDescription(description: std.unittest.mock.StubDescription, atString: std.core.String, prefixString: std.core.String): Unit

    func renderMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Any

    func findMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.collection.ArrayList<std.unittest.mock.Invocation>

    func renderInvocationList(invocations: std.core.Collection<std.unittest.mock.Invocation>): Unit

    func renderInvocationWithLocation(invocation: std.unittest.mock.Invocation, atString: std.core.String): Unit

    func renderInvocation(invocation: std.unittest.mock.Invocation): Unit

    func renderLocation(location: std.unittest.mock.Location): Unit

    func renderDeclarationNameAndKind(info: std.unittest.mock.`internal`.FuncInfo, getterSetterPrefix: Bool): Unit

    func reportArgumentMismatch(invocationMatcher: std.unittest.mock.InvocationMatcher, unmatchedArgumentIndices: std.core.Array<Int64>): Unit

    func renderCardinalityMin(cardinality: std.unittest.mock.Cardinality): Unit

    func renderCardinalityMax(cardinality: std.unittest.mock.Cardinality): Unit

    func renderRequiredActual(actualInvocations: Int64, renderCardinality: () -> Unit): Unit

    func timesStr(i: Int64): Unit

    func indent(body: () -> Unit): Unit

    func line(str: std.core.String): Unit

    func line(body: () -> Unit): Unit

    func listEntry(body: () -> Unit): Unit

    func nl(ensureDotAtTheEnd: Bool): Unit

    func text(str: std.core.String): Unit

    func append(str: std.core.String): Unit

    func userCode(userCode: std.core.ToString): Unit

    func error(errorText: std.core.ToString): Unit

    func success(successText: std.core.ToString): Unit

    func errorLine(text: std.core.String): Unit

    func errorHeader(text: std.core.String): Unit

    func includeContent(content: std.unittest.common.PrettyPrintable): Unit

    let invocationLog: std.unittest.mock.LogSnapshot

    let nameRegistry: std.unittest.mock.MockNameRegistry

    init(invocationLog: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class StubModeViolation <: std.unittest.mock.FailureReport {
    func stubForbidden(stubDescription: std.unittest.mock.StubDescription, sessionName: std.core.String): Nothing

    func statefulStubAction(actionName: std.core.String, stubChain: std.unittest.mock.StubChain): Nothing

    func cardinalitySpecifierNotAllowed(stubChain: std.unittest.mock.StubChain): Nothing

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

    prop cardinality: std.unittest.mock.Cardinality

    func fromStub(stubCall: () -> R, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, objName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.VerifyStatement

    func prepareForVerification(orderedness: std.unittest.mock.Orderedness): Unit

    func setCardinality(newCardinality: std.unittest.mock.Cardinality): std.unittest.mock.VerifyStatement

    func once: std.unittest.mock.VerifyStatement

    func atLeastOnce: std.unittest.mock.VerifyStatement

    func times(expectedTimes: Int64): std.unittest.mock.VerifyStatement

    func times(min: Int64, max: Int64): std.unittest.mock.VerifyStatement

    func atLeastTimes(minTimesExpected: Int64): std.unittest.mock.VerifyStatement

    func never: std.unittest.mock.VerifyStatement

    let invocationMatcher: std.unittest.mock.InvocationMatcher

    init(invocationMatcher: std.unittest.mock.InvocationMatcher) { /* compiled code */ }
}

class ArgumentMatcher {
    var description: std.core.Option<std.core.String>

    var parameterName: std.core.Option<std.core.String>

    func getDescription: std.core.Option<std.core.String>

    func withDescription(description: std.core.String): std.unittest.mock.ArgumentMatcher

    func getParameterName: std.core.Option<std.core.String>

    func forParameter(name: std.core.String): std.unittest.mock.ArgumentMatcher

    func matchesAny(arg: std.core.Any): Bool

    init() { /* compiled code */ }
}

class VerifyMisconfiguration <: std.unittest.mock.FailureReport {
    func fail(reason: std.unittest.mock.VerifyMisconfigurationReason): Nothing

    init() { /* compiled code */ }
}

interface ListeningMatcher {
    prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal>
}

interface ListenerInternal {
    func supplyValue(value: std.core.Any): Unit
}

class TypedMatcher <: std.unittest.mock.ArgumentMatcher {
    func matches(arg: T): Bool

    func matchesAny(arg: std.core.Any): Bool

    let expectedBooleanValue: Bool

    init(expectedBooleanValue: Bool) { /* compiled code */ }
}

class PredicateMatcher <: std.unittest.mock.TypedMatcher<T> & std.unittest.mock.ListeningMatcher {
    func matches(arg: T): Bool

    prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal>

    let predicate: (T) -> Bool

    let expectedBooleanValue: Bool

    let listener: std.core.Option<std.unittest.mock.ListenerInternal>

    init(predicate: (T) -> Bool, expectedBooleanValue: Bool, listener: std.core.Option<std.unittest.mock.ListenerInternal>) { /* compiled code */ }
}

class AnyMatcher <: std.unittest.mock.ArgumentMatcher {
    func matchesAny(_: std.core.Any): Bool

    init() { /* compiled code */ }
}

class Matchers {
    func argThat(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T>

    func argThatNot(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T>

    func argThat(listener: std.unittest.mock.ValueListener<T>, predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T>

    func same(target: T): std.unittest.mock.TypedMatcher<T>

    func eq(target: T): std.unittest.mock.TypedMatcher<T>

    func ofType: std.unittest.mock.TypedMatcher<T>

    func any: std.unittest.mock.AnyMatcher

    func capture(listener: std.unittest.mock.ValueListener<T>): std.unittest.mock.TypedMatcher<T>

    func default(target: T): std.unittest.mock.TypedMatcher<T>
}

extend TypedMatcher {
    func value: T
}

extend AnyMatcher {
    func value: T
}

class Cardinality {
    func check(timesInvoked: Int64): std.unittest.mock.CardinalityCheck

    func once: std.unittest.mock.Cardinality

    func atLeastTimes(minTimes: Int64): std.unittest.mock.Cardinality

    func atLeastOnce: std.unittest.mock.Cardinality

    func times(min: Int64, max: Int64): std.unittest.mock.Cardinality

    func times(timesExactly: Int64): std.unittest.mock.Cardinality

    func never: std.unittest.mock.Cardinality

    let min: Int64

    let max: std.core.Option<Int64>

    init(min: Int64, max: std.core.Option<Int64>) { /* compiled code */ }
}

enum CardinalityCheck {
    | Less

    | Ok

    | More
}

func require(condition: Bool, message: () -> std.core.String): Unit

class ConfigureMock {
    func stubFunction(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, methodName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.MethodActionSelector<TRet>

    func stubGetter(stubCall: () -> TRet, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.GetterActionSelector<TRet>

    func stubSetter(stubCall: () -> Unit, _: () -> TArg, matcher: std.unittest.mock.ArgumentMatcher, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.SetterActionSelector<TArg>

    func createScenario(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.Scenario
}

func requireObject(ref: std.core.Any, name: std.core.String): std.core.Object

class PrettyException <: std.core.Exception & std.unittest.common.PrettyPrintable {
    func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter

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
    func verifyExpectations: Unit

    func checkForMisconfigurationIssues: Unit
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
    func verifyExpectations: std.core.Option<std.unittest.mock.ExpectationFailedReason>

    func getTotalCardinality: std.unittest.mock.Cardinality

    func getTotalInvocationCount: Int64
}

enum ExpectationFailedReason {
    | TooFewInvocationsForStubChain(std.unittest.mock.StubChain, std.unittest.mock.Cardinality, Int64)
}

class ExpectationFailedReport <: std.unittest.mock.FailureReport {
    func fail(log: std.unittest.mock.LogSnapshot, failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): Nothing

    func renderExpectationFailures(failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): std.unittest.common.PrettyText

    func renderExpectationFailedReason(reason: std.unittest.mock.ExpectationFailedReason): Unit

    func renderInvocationListWithHandledByHints(entries: std.core.Array<std.unittest.mock.LogEntry>): Unit

    func findEntriesByStubId(stubId: std.unittest.mock.StubId): std.collection.ArrayList<std.unittest.mock.LogEntry>

    init(log: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class LogSnapshot {
    func invocations: std.core.Array<std.unittest.mock.Invocation>

    let EMPTY: std.unittest.mock.LogSnapshot

    let entries: std.core.Array<std.unittest.mock.LogEntry>

    init(entries: std.core.Array<std.unittest.mock.LogEntry>) { /* compiled code */ }
}

struct LogEntry {
    func wasHandledBy(thatChain: std.unittest.mock.StubChain): Bool

    let invocation: std.unittest.mock.Invocation

    let origin: std.unittest.mock.BehaviourSource

    init(invocation: std.unittest.mock.Invocation, origin: std.unittest.mock.BehaviourSource) { /* compiled code */ }
}

class StubId <: std.core.Object & std.core.Hashable & std.core.Equatable<std.unittest.mock.StubId> {
    func hashCode: Int64

    operator func ==(that: std.unittest.mock.StubId): Bool

    operator func !=(that: std.unittest.mock.StubId): Bool

    let funcInfo: std.unittest.mock.`internal`.FuncInfo

    let receiverId: UInt64

    init(funcInfo: std.unittest.mock.`internal`.FuncInfo, receiverId: UInt64) { /* compiled code */ }
}

class MockNameRegistry {
    let idToName: std.collection.HashMap<UInt64, std.core.Option<std.core.String>>

    func submit(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Option<std.core.Option<std.core.String>>

    func `get`(objId: UInt64): std.core.Option<std.core.String>

    let EMPTY: std.unittest.mock.MockNameRegistry

    init(matchers: std.core.Array<std.unittest.mock.InvocationMatcher>) { /* compiled code */ }
}

class InvocationMatcher {
    let positionalMatchers: std.collection.ArrayList<std.unittest.mock.ArgumentMatcher>

    func tryMatch(invocation: std.unittest.mock.Invocation): std.unittest.mock.MatchStatus

    func matches(invocation: std.unittest.mock.Invocation): Bool

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
    func getEntries: std.core.Array<std.unittest.mock.TraceEntry>

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

    func getExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription): std.core.Any

    func setExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription, value: std.core.Any): Unit

    func getAutoFieldValue(id: std.unittest.mock.AutoFieldId): std.core.Option<std.core.Any>

    func setAutoFieldValue(id: std.unittest.mock.AutoFieldId, value: std.core.Any): Unit

    init() { /* compiled code */ }
}

struct AutoFieldId <: std.core.Hashable & std.core.Equatable<std.unittest.mock.AutoFieldId> {
    init(mockObject: std.unittest.mock.MockObject, memberName: std.core.String) { /* compiled code */ }

    func hashCode: Int64

    operator func ==(that: std.unittest.mock.AutoFieldId): Bool

    operator func !=(that: std.unittest.mock.AutoFieldId): Bool

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
    func errorReadingField(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException

    func valueNotSet(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException

    func mustUseStubs(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException

    init() { /* compiled code */ }
}

let _FRAMEWORK: std.unittest.mock.MockFramework

class MockFramework {
    let sessions: std.collection.ArrayList<std.unittest.mock.MockSession>

    var recording: std.core.Option<std.unittest.mock.RecordingHandler>

    let globalLock: std.sync.ReentrantMutex

    let objCounter: std.sync.AtomicUInt64

    init() { /* compiled code */ }

    func openSession(name: std.core.String, sessionKind: std.unittest.mock.MockSessionKind): Unit

    func closeSession: Unit

    func generateUniqueId: UInt64

    func session(body: (std.unittest.mock.MockSession) -> T): T

    func recordCall(recordingInfo: std.unittest.mock.RecordingInfo, dummyCall: () -> R): std.unittest.mock.RecordedCall

    func handleSync(call: std.unittest.mock.`internal`.Call, objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>): std.unittest.mock.`internal`.OnCall

    func registerObject(mockObject: std.unittest.mock.MockObject): Unit

    func findObjectIdsByRefs(referencesToFind: std.core.Array<std.core.Object>): std.collection.Set<UInt64>

    func throwNotAMockObject: Nothing
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

    func record(call: std.unittest.mock.`internal`.Call, object: std.core.Option<std.unittest.mock.MockObject>): Unit

    func getRecorded: std.unittest.mock.RecordedCall

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
    func hasMode(mode: std.unittest.mock.StubMode): Bool
}

extend Invocation {
    prop isDefaultEnabled: Bool

    prop canReturnDefaultValue: Bool
}

class MockScopeViolation <: std.unittest.mock.FailureReport {
    let HINT: std.core.String

    func reportInaccessible(createdAtSessionName: std.core.Option<std.core.String>, whereSessionName: std.core.Option<std.core.String>): Nothing

    init() { /* compiled code */ }
}

enum SessionHandleResult {
    | PerformScenario(std.unittest.mock.Scenario)

    | Unhandled

    | InvocationLimitExceeded(std.unittest.mock.StubChain)
}

extend MockSession {
    func `handle`(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>): std.unittest.mock.SessionHandleResult
}

extend StubChain {
    func runListeners(invocation: std.unittest.mock.Invocation): Unit
}

enum InternalError {
    | InconsistentState(std.core.String)

    | AssertionFailed(std.core.String)

    | Unimplemented(std.core.String)
}

func internalError(e: std.unittest.mock.InternalError): Nothing

func internalError(assertion: std.core.String): Nothing

func illegalInput(text: std.core.String): Nothing

enum BehaviourSource {
    | Stub(std.unittest.mock.StubChain)

    | Default

    | Failure(std.unittest.mock.PrettyException)
}

class MutableLog {
    let entries: std.collection.ArrayList<std.unittest.mock.LogEntry>

    var lastClearIndex: Int64

    func newEntry(entry: std.unittest.mock.LogEntry): Unit

    func clear: Unit

    func snapshotSinceLastClear(scope: std.collection.Set<UInt64>): std.unittest.mock.LogSnapshot

    func fullLog: std.unittest.mock.LogSnapshot

    func validateLog: Unit

    init() { /* compiled code */ }
}

class LogCorruptedReport <: std.unittest.mock.FailureReport {
    func fail(originalMessage: std.unittest.common.PrettyPrintable): Nothing

    init() { /* compiled code */ }
}

struct TraceEntry {
    let filePath: std.core.String

    let lineNumber: Int64

    init(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let UNKNOWN_LOCATION: std.unittest.mock.LocationImpl

class LocationImpl <: std.unittest.mock.Location {
    func getEntries: std.core.Array<std.unittest.mock.TraceEntry>

    let entries: std.core.Array<std.unittest.mock.TraceEntry>

    init(entries: std.core.Array<std.unittest.mock.TraceEntry>) { /* compiled code */ }
}

let UNKNOWN_LOCATION_STRING: std.core.String

class SimpleLocation <: std.unittest.mock.Location {
    func getEntries: std.core.Array<std.unittest.mock.TraceEntry>

    let filePath: std.core.String

    let lineNumber: Int64

    init(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let MOCKED_CLASS_NAME_FRAGMENT: std.core.String

func locationFromTrace: std.unittest.mock.Location

extend Location {
    func forceLineNumber(lineNum: Int64): std.unittest.mock.Location

    func fileNameAndLine: std.core.String
}

class NoneMatcher <: std.unittest.mock.ArgumentMatcher {
    func matchesAny(arg: std.core.Any): Bool

    init() { /* compiled code */ }
}

extend Matchers {
    func none: std.unittest.mock.NoneMatcher
}

extend NoneMatcher {
    func value: std.core.Option<T>
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
    func fail(info: std.unittest.mock.RecordingInfo, possibleReasons: std.core.Array<std.unittest.mock.RecordingFailureReason>): Nothing

    init() { /* compiled code */ }
}

enum RuntimeFailureReason {
    | UnstubbedDeclaration(std.unittest.mock.`internal`.FuncInfo)

    | NoneMatchedArguments(std.core.Array<(std.unittest.mock.StubChain, std.core.Array<Int64>)>)

    | TooManyInvocationsForStubChain(std.unittest.mock.StubChain)

    | UnwantedInteraction(std.unittest.mock.StubChain)
}

func unhandledInvocationReport(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException

func invocationLimitExceededReport(currentInvocation: std.unittest.mock.Invocation, chain: std.unittest.mock.StubChain, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException

func runtimeUnwantedInteraction(currentInvocation: std.unittest.mock.Invocation, sc: std.unittest.mock.Scenario, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException

class RuntimeFailureReport <: std.unittest.mock.FailureReport {
    func render(invocation: std.unittest.mock.Invocation, reason: std.unittest.mock.RuntimeFailureReason): std.unittest.common.PrettyText

    func findInvocationsHandledByChain(stubChain: std.unittest.mock.StubChain): std.core.Array<std.unittest.mock.Invocation>

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
    func value: std.core.Option<std.core.Any>
}

extend Option <: UntypedOption {
    func value: std.core.Option<std.core.Any>
}

func unwrapValue(value: std.core.Any): std.core.Option<T>

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

func performVerification(statements: std.core.Array<std.unittest.mock.VerifyStatement>, orderedness: std.unittest.mock.Orderedness, exhaustiveness: std.unittest.mock.Exhaustiveness): Unit

func verifyNoInteractions(objectRefs: std.core.Array<std.core.Object>): Unit

func ensureDistinctReferences(refs: std.core.Array<std.core.Object>): Unit

func collectObjectsIds(statements: std.core.Array<std.unittest.mock.VerifyStatement>): std.collection.HashSet<UInt64>

func checkMatchersConsistency(statement: std.unittest.mock.VerifyStatement): Unit

class VerificationFailureReport <: std.unittest.mock.FailureReport {
    func fail(reason: std.unittest.mock.VerificationFailedReason): Nothing

    init(logSnapshot: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

class Verify {
    func that(statement: std.unittest.mock.VerifyStatement): Unit

    func unordered(exhaustive: std.unittest.mock.Exhaustiveness, collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit

    func unordered(collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit

    func unordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit

    func unordered(exhaustive: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit

    func ordered(collectStatements: (std.unittest.mock.OrderedVerifier) -> Unit): Unit

    func ordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit

    func clearInvocationLog: Unit

    func noInteractions(mockObjects: std.core.Array<std.core.Object>): Unit

    init() { /* compiled code */ }
}

class UnorderedVerifier {
    let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.UnorderedVerifier

    init() { /* compiled code */ }
}

class OrderedVerifier {
    let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.OrderedVerifier

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

    func goToNextStatement: Bool

    func hasMoreStatements: Bool

    func goToNextInvocation: Bool

    func check(count: Int64): std.unittest.mock.CardinalityCheck

    let statements: std.core.Iterable<std.unittest.mock.VerifyStatement>

    let invocations: std.core.Array<std.unittest.mock.Invocation>

    init(statements: std.core.Iterable<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>) { /* compiled code */ }
}

func verifyExhaustiveOrderedIteration(iterator: std.unittest.mock.VerifyIterator): std.core.Option<std.unittest.mock.VerificationResult>

func verifyExhaustiveOrdered(statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult

extend VerifyIterator {
    func tooFewInvocations(count: Int64): std.unittest.mock.VerificationResult

    func tooManyInvocations(count: Int64): std.unittest.mock.VerificationResult

    func nonDisjointStatements: std.unittest.mock.VerificationResult

    func unmatchedStatements: std.unittest.mock.VerificationResult

    func unmatchedInvocations: std.unittest.mock.VerificationResult

    func unexpectedInvocation: std.unittest.mock.VerificationResult

    func leftoverStatements: std.core.Array<std.unittest.mock.VerifyStatement>
}

func verifyUnordered(exhastiveness: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult

func batchMatch(statements: std.core.Array<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>): std.unittest.mock.BatchMatchResult

enum BatchMatchResult {
    | DisjointMatching(std.core.Array<std.core.Option<std.unittest.mock.VerifyStatement>>, std.core.Array<Int64>)

    | Ambiguity(std.unittest.mock.VerifyStatement, std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)
}
