// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.mock

import std.collection.ArrayList
import std.core.*
import std.unittest.mock.internal.*
import std.unittest.common.*
import std.collection.*
import std.unittest.common.PrettyText
import std.collection.HashMap
import std.sync.*
import std.unittest.mock.internal.FuncInfo
import std.unittest.common.PrettyPrintable
import std.unittest.mock.internal.ParameterInfo
import std.collection.enumerate
import std.fs.Path

@Frozen
public func mock<T>(): T { /* compiled code */ }

internal enum MockKind {
    | Mock

    | Spy
}

public enum StubMode {
    | ReturnsDefaults

    | SyntheticFields
}

internal class CallHandlerImpl <: std.core.Object & std.unittest.mock.internal.CallHandler {
    public func onCall(call: std.unittest.mock.internal.Call): std.unittest.mock.internal.OnCall { /* compiled code */ }

    public func requireMockObject<T>(obj: T): T { /* compiled code */ }

    private let objId: UInt64

    private let objectCreatedAtSessionName: std.core.Option<std.core.String>

    CallHandlerImpl(objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>) { /* compiled code */ }
}

internal func create<T>(kind: std.unittest.mock.MockKind, modes: std.core.Array<std.unittest.mock.StubMode>, constructor: (std.unittest.mock.CallHandlerImpl) -> T): T { /* compiled code */ }

@Frozen
public func mock<T>(modes: std.core.Array<std.unittest.mock.StubMode>): T { /* compiled code */ }

@Frozen
public func spy<T>(objectToSpyOn: T): T { /* compiled code */ }

public interface ValueListener<T> {
    public open func lastValue(): std.core.Option<T>

    public open func allValues(): std.core.Array<T>

    public open func addCallback(callback: (T) -> Unit): Unit

    public static func new(): std.unittest.mock.ValueListener<T> { /* compiled code */ }

    public static func onEach(callback: (T) -> Unit): std.unittest.mock.ValueListener<T> { /* compiled code */ }
}

internal class ValueListenerImpl<T> <: std.core.Object & std.unittest.mock.ValueListener<T> & std.unittest.mock.ListenerInternal {
    private let callBacks: std.collection.ArrayList<(T) -> Unit> = COMPILED_CODE /* compiled code */

    private let values: std.collection.ArrayList<T> = COMPILED_CODE /* compiled code */

    init(callBack: (T) -> Unit) { /* compiled code */ }

    public func lastValue(): std.core.Option<T> { /* compiled code */ }

    public func allValues(): std.core.Array<T> { /* compiled code */ }

    public func addCallback(callBack: (T) -> Unit): Unit { /* compiled code */ }

    public override func supplyValue(value: std.core.Any): Unit { /* compiled code */ }

    ValueListenerImpl() { /* compiled code */ }
}

public abstract open class ActionSelector {
    internal func mustBeSpy(actionName: std.core.String): Unit { /* compiled code */ }

    internal func throwsImpl(exception: std.core.Exception): Unit { /* compiled code */ }

    internal func throwsFactoryImpl(exceptionFactory: () -> std.core.Exception): Unit { /* compiled code */ }

    internal func returnsImpl<TRet>(value: TRet): Unit { /* compiled code */ }

    internal func returnsFactoryImpl<TRet>(factory: () -> TRet): Unit { /* compiled code */ }

    internal func returnsCollectionImpl<TRet>(values: std.core.Collection<TRet>): Unit { /* compiled code */ }

    public func fails(): Unit { /* compiled code */ }

    internal let sc: std.unittest.mock.Scenario

    ActionSelector(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

internal class Scenario {
    private var status: std.unittest.mock.ScenarioStatus

    internal func setAction(selectedAction: std.unittest.mock.Action): Unit { /* compiled code */ }

    internal func setCardinality(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    internal func tryRegisterInvocation(): Bool { /* compiled code */ }

    internal func activate(): Unit { /* compiled code */ }

    internal prop invocationCount: Int64 {
        get() { /* compiled code */ }
    }

    internal prop action: std.unittest.mock.Action {
        get() { /* compiled code */ }
    }

    internal prop cardinality: std.unittest.mock.Cardinality {
        get() { /* compiled code */ }
    }

    internal let parentChain: std.unittest.mock.StubChain

    Scenario(parentChain: std.unittest.mock.StubChain) { /* compiled code */ }
}

public class MethodActionSelector<TRet> <: std.unittest.mock.ActionSelector {
    internal func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    internal func continuation(): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    public func callsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<TRet>> { /* compiled code */ }

    MethodActionSelector(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

public class CardinalitySelector<A> {
    public func anyTimes(): Unit { /* compiled code */ }

    public func once(): std.unittest.mock.Continuation<A> { /* compiled code */ }

    public func atLeastOnce(): Unit { /* compiled code */ }

    public func times(expectedTimes: Int64): std.unittest.mock.Continuation<A> { /* compiled code */ }

    public func times(min!: Int64, max!: Int64): Unit { /* compiled code */ }

    public func atLeastTimes(minTimesExpected: Int64): Unit { /* compiled code */ }

    internal let sc: std.unittest.mock.Scenario

    internal let factory: (std.unittest.mock.Scenario) -> A

    CardinalitySelector(sc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

public class Continuation<A> {
    public func then(): A { /* compiled code */ }

    internal let previousSc: std.unittest.mock.Scenario

    internal let factory: (std.unittest.mock.Scenario) -> A

    Continuation(previousSc: std.unittest.mock.Scenario, factory: (std.unittest.mock.Scenario) -> A) { /* compiled code */ }
}

public class GetterActionSelector<TRet> <: std.unittest.mock.ActionSelector {
    internal func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    internal func continuation(): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func returns(value: TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func returns(valueFactory: () -> TRet): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func returnsConsecutively(values: std.core.Array<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func returnsConsecutively(values: std.collection.ArrayList<TRet>): std.unittest.mock.Continuation<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func getsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    public func getsField(field: std.unittest.mock.SyntheticField<TRet>): std.unittest.mock.CardinalitySelector<std.unittest.mock.GetterActionSelector<TRet>> { /* compiled code */ }

    GetterActionSelector(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

public class SyntheticField<T> {
    public static func create(initialValue!: T): std.unittest.mock.SyntheticField<T> { /* compiled code */ }

    internal let description: std.unittest.mock.SyntheticFieldDescription

    SyntheticField(description: std.unittest.mock.SyntheticFieldDescription) { /* compiled code */ }
}

public class SetterActionSelector<TArg> <: std.unittest.mock.ActionSelector {
    internal func cardinalitySelector(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    public func throws(exception: std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    public func throws(exceptionFactory: () -> std.core.Exception): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    public func doesNothing(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    public func setsOriginal(): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    public func setsField(field: std.unittest.mock.SyntheticField<TArg>): std.unittest.mock.CardinalitySelector<std.unittest.mock.SetterActionSelector<TArg>> { /* compiled code */ }

    SetterActionSelector(sc: std.unittest.mock.Scenario) { /* compiled code */ }
}

extend std.unittest.mock.MethodActionSelector<Unit> {
    public func returns(): std.unittest.mock.CardinalitySelector<std.unittest.mock.MethodActionSelector<Unit>> { /* compiled code */ }
}

internal enum StubMisconfigurationReason {
    | ActionNotSpecified

    | CardinalityAlreadySet

    | ActionAlreadySet

    | CannotModifyActive

    | OnlySpyCanCallOriginal(std.core.String)
}

internal class StubMisconfiguration <: std.unittest.mock.FailureReport {
    internal func fail(stub: std.unittest.mock.StubChain, reason: std.unittest.mock.StubMisconfigurationReason): Nothing { /* compiled code */ }

    internal func stubConfigurationHint(reason: std.unittest.mock.StubMisconfigurationReason): Unit { /* compiled code */ }

    StubMisconfiguration() { /* compiled code */ }
}

internal class StubChain {
    private var wasActivated: Bool

    internal let scenarios: std.collection.ArrayList<std.unittest.mock.Scenario>

    internal func addScenario(_: std.unittest.mock.MockSession): std.unittest.mock.Scenario { /* compiled code */ }

    internal func ensureActivated(): Unit { /* compiled code */ }

    internal func isVerifiable(): Bool { /* compiled code */ }

    internal let invocationMatcher: std.unittest.mock.InvocationMatcher

    internal let objectKind: std.unittest.mock.MockKind

    internal let parentSession: std.unittest.mock.MockSession

    StubChain(invocationMatcher: std.unittest.mock.InvocationMatcher, objectKind: std.unittest.mock.MockKind, parentSession: std.unittest.mock.MockSession) { /* compiled code */ }
}

internal open class FailureReport {
    private let textBuilder: std.unittest.common.PrettyText

    private var lastSymbol: Rune

    init() { /* compiled code */ }

    internal func build(body: () -> Unit): std.unittest.common.PrettyText { /* compiled code */ }

    internal func renderStatement(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

    internal func renderStub(stubChain: std.unittest.mock.StubChain): Unit { /* compiled code */ }

    internal func renderMatcher(invocationMatcher: std.unittest.mock.InvocationMatcher, atString!: std.core.String = COMPILED_CODE /* compiled code */, prefixString!: std.core.String = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func renderStubDescription(description: std.unittest.mock.StubDescription, atString!: std.core.String, prefixString!: std.core.String): Unit { /* compiled code */ }

    internal func renderMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Any { /* compiled code */ }

    internal func findMatchingInvocations(invocationMatcher: std.unittest.mock.InvocationMatcher): std.collection.ArrayList<std.unittest.mock.Invocation> { /* compiled code */ }

    internal func renderInvocationList(invocations: std.core.Collection<std.unittest.mock.Invocation>): Unit { /* compiled code */ }

    internal func renderInvocationWithLocation(invocation: std.unittest.mock.Invocation, atString!: std.core.String = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func renderInvocation(invocation: std.unittest.mock.Invocation): Unit { /* compiled code */ }

    internal func renderLocation(location: std.unittest.mock.Location): Unit { /* compiled code */ }

    internal func renderDeclarationNameAndKind(info: std.unittest.mock.internal.FuncInfo, getterSetterPrefix!: Bool): Unit { /* compiled code */ }

    internal func reportArgumentMismatch(invocationMatcher: std.unittest.mock.InvocationMatcher, unmatchedArgumentIndices: std.core.Array<Int64>): Unit { /* compiled code */ }

    internal func renderCardinalityMin(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    internal func renderCardinalityMax(cardinality: std.unittest.mock.Cardinality): Unit { /* compiled code */ }

    internal func renderRequiredActual(actualInvocations: Int64, renderCardinality: () -> Unit): Unit { /* compiled code */ }

    internal func timesStr(i: Int64): Unit { /* compiled code */ }

    internal func indent(body: () -> Unit): Unit { /* compiled code */ }

    internal func line(str: std.core.String): Unit { /* compiled code */ }

    internal func line(body: () -> Unit): Unit { /* compiled code */ }

    internal func listEntry(body: () -> Unit): Unit { /* compiled code */ }

    internal func nl(ensureDotAtTheEnd!: Bool = COMPILED_CODE /* compiled code */): Unit { /* compiled code */ }

    internal func text(str: std.core.String): Unit { /* compiled code */ }

    internal func append(str: std.core.String): Unit { /* compiled code */ }

    internal func userCode(userCode: std.core.ToString): Unit { /* compiled code */ }

    internal func error(errorText: std.core.ToString): Unit { /* compiled code */ }

    internal func success(successText: std.core.ToString): Unit { /* compiled code */ }

    internal func errorLine(text: std.core.String): Unit { /* compiled code */ }

    internal func errorHeader(text: std.core.String): Unit { /* compiled code */ }

    internal func includeContent(content: std.unittest.common.PrettyPrintable): Unit { /* compiled code */ }

    protected let invocationLog: std.unittest.mock.LogSnapshot

    private let nameRegistry: std.unittest.mock.MockNameRegistry

    FailureReport(invocationLog: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

internal class StubModeViolation <: std.unittest.mock.FailureReport {
    internal func stubForbidden(stubDescription: std.unittest.mock.StubDescription, sessionName: std.core.String): Nothing { /* compiled code */ }

    internal func statefulStubAction(actionName: std.core.String, stubChain: std.unittest.mock.StubChain): Nothing { /* compiled code */ }

    internal func cardinalitySpecifierNotAllowed(stubChain: std.unittest.mock.StubChain): Nothing { /* compiled code */ }

    StubModeViolation() { /* compiled code */ }
}

internal struct StubDescription {
    internal let fullCall: std.core.String

    internal let prefixRefName: std.core.Option<std.core.String>

    internal let location: std.unittest.mock.Location

    StubDescription(fullCall: std.core.String, prefixRefName: std.core.Option<std.core.String>, location: std.unittest.mock.Location) { /* compiled code */ }
}

internal enum VerifyMisconfigurationReason {
    | StatementUsedTwice(std.unittest.mock.VerifyStatement)

    | StatementNotUsed(std.unittest.mock.VerifyStatement)

    | ArgumentCaptureNotAllowed(std.unittest.mock.VerifyStatement, std.unittest.mock.ArgumentMatcher)

    | CardinalityCannotBeReassigned(std.unittest.mock.VerifyStatement)

    | EmptyVerifyBlock
}

public class VerifyStatement {
    private var _cardinality: std.core.Option<std.unittest.mock.Cardinality>

    internal var isUsed: Bool

    internal prop cardinality: std.unittest.mock.Cardinality {
        get() { /* compiled code */ }
    }

    public static func fromStub<R>(stubCall: () -> R, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, objName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    internal func prepareForVerification(orderedness: std.unittest.mock.Orderedness): Unit { /* compiled code */ }

    internal func setCardinality(newCardinality: std.unittest.mock.Cardinality): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func once(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func atLeastOnce(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func times(expectedTimes: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func times(min!: Int64, max!: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func atLeastTimes(minTimesExpected: Int64): std.unittest.mock.VerifyStatement { /* compiled code */ }

    public func never(): std.unittest.mock.VerifyStatement { /* compiled code */ }

    internal let invocationMatcher: std.unittest.mock.InvocationMatcher

    VerifyStatement(invocationMatcher: std.unittest.mock.InvocationMatcher) { /* compiled code */ }
}

public abstract class ArgumentMatcher {
    private var description: std.core.Option<std.core.String>

    private var parameterName: std.core.Option<std.core.String>

    internal func getDescription(): std.core.Option<std.core.String> { /* compiled code */ }

    public func withDescription(description: std.core.String): std.unittest.mock.ArgumentMatcher { /* compiled code */ }

    internal func getParameterName(): std.core.Option<std.core.String> { /* compiled code */ }

    public func forParameter(name: std.core.String): std.unittest.mock.ArgumentMatcher { /* compiled code */ }

    public func matchesAny(arg: std.core.Any): Bool

    ArgumentMatcher() { /* compiled code */ }
}

internal class VerifyMisconfiguration <: std.unittest.mock.FailureReport {
    internal func fail(reason: std.unittest.mock.VerifyMisconfigurationReason): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

internal interface ListeningMatcher {
    public open prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal>
}

internal interface ListenerInternal {
    public open func supplyValue(value: std.core.Any): Unit
}

public abstract class TypedMatcher<T> <: std.unittest.mock.ArgumentMatcher {
    public func matches(arg: T): Bool

    public override func matchesAny(arg: std.core.Any): Bool { /* compiled code */ }

    private let expectedBooleanValue: Bool

    TypedMatcher(expectedBooleanValue: Bool) { /* compiled code */ }
}

internal class PredicateMatcher<T> <: std.unittest.mock.TypedMatcher<T> & std.unittest.mock.ListeningMatcher {
    public override func matches(arg: T): Bool { /* compiled code */ }

    public prop valueListener: std.core.Option<std.unittest.mock.ListenerInternal> {
        get() { /* compiled code */ }
    }

    private let predicate: (T) -> Bool

    private let expectedBooleanValue: Bool

    private let listener: std.core.Option<std.unittest.mock.ListenerInternal>

    PredicateMatcher(predicate: (T) -> Bool, expectedBooleanValue!: Bool = COMPILED_CODE /* compiled code */, listener!: std.core.Option<std.unittest.mock.ListenerInternal> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

public class AnyMatcher <: std.unittest.mock.ArgumentMatcher {
    public func matchesAny(_: std.core.Any): Bool { /* compiled code */ }

    init() { /* compiled code */ }
}

public class Matchers {
    public static func argThat<T>(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func argThatNot<T>(predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func argThat<T>(listener: std.unittest.mock.ValueListener<T>, predicate: (T) -> Bool): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func same<T>(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func eq<T>(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func ofType<T>(): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func any(): std.unittest.mock.AnyMatcher { /* compiled code */ }

    public static func capture<T>(listener: std.unittest.mock.ValueListener<T>): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }

    public static func default<T>(target: T): std.unittest.mock.TypedMatcher<T> { /* compiled code */ }
}

extend std.unittest.mock.TypedMatcher<T> {
    public func value(): T { /* compiled code */ }
}

extend std.unittest.mock.AnyMatcher {
    public func value<T>(): T { /* compiled code */ }
}

internal class Cardinality {
    internal func check(timesInvoked: Int64): std.unittest.mock.CardinalityCheck { /* compiled code */ }

    public static func once(): std.unittest.mock.Cardinality { /* compiled code */ }

    public static func atLeastTimes(minTimes: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    public static func atLeastOnce(): std.unittest.mock.Cardinality { /* compiled code */ }

    public static func times(min!: Int64, max!: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    public static func times(timesExactly: Int64): std.unittest.mock.Cardinality { /* compiled code */ }

    public static func never(): std.unittest.mock.Cardinality { /* compiled code */ }

    internal let min: Int64

    internal let max: std.core.Option<Int64>

    Cardinality(min!: Int64, max!: std.core.Option<Int64> = COMPILED_CODE /* compiled code */) { /* compiled code */ }
}

internal enum CardinalityCheck {
    | Less

    | Ok

    | More
}

internal func require(condition: Bool, message: () -> std.core.String): Unit { /* compiled code */ }

public class ConfigureMock {
    public static func stubFunction<TRet>(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, methodName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.MethodActionSelector<TRet> { /* compiled code */ }

    public static func stubGetter<TRet>(stubCall: () -> TRet, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.GetterActionSelector<TRet> { /* compiled code */ }

    public static func stubSetter<TArg>(stubCall: () -> Unit, _: () -> TArg, matcher: std.unittest.mock.ArgumentMatcher, prefixRefName: std.core.Option<std.core.String>, fieldOrPropertyName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.SetterActionSelector<TArg> { /* compiled code */ }

    internal static func createScenario<TRet>(stubCall: () -> TRet, matchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, prefixRefName: std.core.Option<std.core.String>, declarationName: std.core.String, callDescription: std.core.String, lineNumber: Int64): std.unittest.mock.Scenario { /* compiled code */ }
}

internal func requireObject(ref: std.core.Any, name: std.core.String): std.core.Object { /* compiled code */ }

public abstract class PrettyException <: std.core.Exception & std.unittest.common.PrettyPrintable {
    public func pprint(to: std.unittest.common.PrettyPrinter): std.unittest.common.PrettyPrinter { /* compiled code */ }

    private let text: std.unittest.common.PrettyText

    PrettyException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class MockFrameworkInternalError <: std.unittest.mock.PrettyException {
    MockFrameworkInternalError(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class MockFrameworkException <: std.unittest.mock.PrettyException {
    MockFrameworkException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public open class ExpectationFailedException <: std.unittest.mock.PrettyException {
    ExpectationFailedException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class UnnecessaryStubbingException <: std.unittest.mock.PrettyException {
    UnnecessaryStubbingException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class UnstubbedInvocationException <: std.unittest.mock.PrettyException {
    UnstubbedInvocationException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class UnhandledCallException <: std.unittest.mock.PrettyException {
    UnhandledCallException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

public class VerificationFailedException <: std.unittest.mock.PrettyException {
    VerificationFailedException(text: std.unittest.common.PrettyText) { /* compiled code */ }
}

extend std.unittest.mock.MockSession {
    internal func verifyExpectations(): Unit { /* compiled code */ }

    internal func checkForMisconfigurationIssues(): Unit { /* compiled code */ }
}

internal class MockSession {
    internal let invocationLog: std.unittest.mock.MutableLog

    internal let chainsByStubId: std.collection.HashMap<std.unittest.mock.StubId, std.collection.ArrayList<std.unittest.mock.StubChain>>

    internal let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    internal let objectRegistry: std.collection.HashMap<UInt64, std.unittest.mock.MockObject>

    internal let fieldStorage: std.unittest.mock.FieldStorage

    internal let name: std.core.String

    internal let sessionKind: std.unittest.mock.MockSessionKind

    MockSession(name: std.core.String, sessionKind: std.unittest.mock.MockSessionKind) { /* compiled code */ }
}

extend std.unittest.mock.StubChain {
    internal func verifyExpectations(): std.core.Option<std.unittest.mock.ExpectationFailedReason> { /* compiled code */ }

    internal func getTotalCardinality(): std.unittest.mock.Cardinality { /* compiled code */ }

    internal func getTotalInvocationCount(): Int64 { /* compiled code */ }
}

internal enum ExpectationFailedReason {
    | TooFewInvocationsForStubChain(std.unittest.mock.StubChain, std.unittest.mock.Cardinality, Int64)
}

internal class ExpectationFailedReport <: std.unittest.mock.FailureReport {
    internal static func fail(log: std.unittest.mock.LogSnapshot, failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): Nothing { /* compiled code */ }

    internal func renderExpectationFailures(failures: std.collection.ArrayList<std.unittest.mock.ExpectationFailedReason>): std.unittest.common.PrettyText { /* compiled code */ }

    internal func renderExpectationFailedReason(reason: std.unittest.mock.ExpectationFailedReason): Unit { /* compiled code */ }

    internal func renderInvocationListWithHandledByHints(entries: std.core.Array<std.unittest.mock.LogEntry>): Unit { /* compiled code */ }

    internal func findEntriesByStubId(stubId: std.unittest.mock.StubId): std.collection.ArrayList<std.unittest.mock.LogEntry> { /* compiled code */ }

    ExpectationFailedReport(log: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

internal class LogSnapshot {
    internal func invocations(): std.core.Array<std.unittest.mock.Invocation> { /* compiled code */ }

    internal static let EMPTY: std.unittest.mock.LogSnapshot

    internal let entries: std.core.Array<std.unittest.mock.LogEntry>

    LogSnapshot(entries: std.core.Array<std.unittest.mock.LogEntry>) { /* compiled code */ }
}

internal struct LogEntry {
    internal func wasHandledBy(thatChain: std.unittest.mock.StubChain): Bool { /* compiled code */ }

    internal let invocation: std.unittest.mock.Invocation

    internal let origin: std.unittest.mock.BehaviourSource

    LogEntry(invocation: std.unittest.mock.Invocation, origin: std.unittest.mock.BehaviourSource) { /* compiled code */ }
}

internal class StubId <: std.core.Object & std.core.Hashable & std.core.Equatable<std.unittest.mock.StubId> {
    public func hashCode(): Int64 { /* compiled code */ }

    public operator func ==(that: std.unittest.mock.StubId): Bool { /* compiled code */ }

    public operator func !=(that: std.unittest.mock.StubId): Bool { /* compiled code */ }

    internal let funcInfo: std.unittest.mock.internal.FuncInfo

    internal let receiverId: UInt64

    StubId(funcInfo: std.unittest.mock.internal.FuncInfo, receiverId: UInt64) { /* compiled code */ }
}

internal class MockNameRegistry {
    private let idToName: std.collection.HashMap<UInt64, std.core.Option<std.core.String>>

    internal func submit(invocationMatcher: std.unittest.mock.InvocationMatcher): std.core.Option<std.core.Option<std.core.String>> { /* compiled code */ }

    internal func get(objId: UInt64): std.core.Option<std.core.String> { /* compiled code */ }

    internal static let EMPTY: std.unittest.mock.MockNameRegistry

    MockNameRegistry(matchers: std.core.Array<std.unittest.mock.InvocationMatcher>) { /* compiled code */ }
}

internal class InvocationMatcher {
    internal let positionalMatchers: std.collection.ArrayList<std.unittest.mock.ArgumentMatcher>

    internal func tryMatch(invocation: std.unittest.mock.Invocation): std.unittest.mock.MatchStatus { /* compiled code */ }

    internal func matches(invocation: std.unittest.mock.Invocation): Bool { /* compiled code */ }

    internal let typeParameters: std.core.Array<std.core.String>

    internal let stubId: std.unittest.mock.StubId

    internal let description: std.unittest.mock.StubDescription

    InvocationMatcher(argumentMatchers: std.core.Array<std.unittest.mock.ArgumentMatcher>, typeParameters: std.core.Array<std.core.String>, stubId: std.unittest.mock.StubId, description: std.unittest.mock.StubDescription) { /* compiled code */ }
}

internal class Invocation {
    internal let stubId: std.unittest.mock.StubId

    internal let mockObject: std.core.Option<std.unittest.mock.MockObject>

    internal let call: std.unittest.mock.internal.Call

    internal let location: std.unittest.mock.Location

    Invocation(mockObject: std.core.Option<std.unittest.mock.MockObject>, call: std.unittest.mock.internal.Call, location: std.unittest.mock.Location) { /* compiled code */ }
}

internal abstract class Location {
    public func getEntries(): std.core.Array<std.unittest.mock.TraceEntry>

    init() { /* compiled code */ }
}

internal class SyntheticFieldDescription {
    internal let id: UInt64

    internal let initialValue: std.core.Any

    SyntheticFieldDescription(initialValue: std.core.Any) { /* compiled code */ }
}

internal class FieldStorage {
    private let explicitFields: std.collection.HashMap<UInt64, std.core.Box<std.core.Any>>

    private let autoFields: std.collection.HashMap<std.unittest.mock.AutoFieldId, std.core.Box<std.core.Any>>

    internal func getExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription): std.core.Any { /* compiled code */ }

    internal func setExplicitFieldValue(fieldDescription: std.unittest.mock.SyntheticFieldDescription, value: std.core.Any): Unit { /* compiled code */ }

    internal func getAutoFieldValue(id: std.unittest.mock.AutoFieldId): std.core.Option<std.core.Any> { /* compiled code */ }

    internal func setAutoFieldValue(id: std.unittest.mock.AutoFieldId, value: std.core.Any): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal struct AutoFieldId <: std.core.Hashable & std.core.Equatable<std.unittest.mock.AutoFieldId> {
    init(mockObject: std.unittest.mock.MockObject, memberName: std.core.String) { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public operator func ==(that: std.unittest.mock.AutoFieldId): Bool { /* compiled code */ }

    public operator func !=(that: std.unittest.mock.AutoFieldId): Bool { /* compiled code */ }

    private let mockObjectId: UInt64

    private let memberName: std.core.String

    AutoFieldId(mockObjectId: UInt64, memberName: std.core.String) { /* compiled code */ }
}

internal class MockObject {
    internal let id: UInt64

    internal let ref: std.core.Object

    internal let kind: std.unittest.mock.MockKind

    internal let modes: std.core.Array<std.unittest.mock.StubMode>

    internal let sessionName: std.core.Option<std.core.String>

    MockObject(id: UInt64, ref: std.core.Object, kind: std.unittest.mock.MockKind, modes: std.core.Array<std.unittest.mock.StubMode>, sessionName: std.core.Option<std.core.String>) { /* compiled code */ }
}

internal class FieldsErrorReport <: std.unittest.mock.FailureReport {
    internal func errorReadingField(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    internal func valueNotSet(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    internal func mustUseStubs(invocation: std.unittest.mock.Invocation): std.unittest.mock.PrettyException { /* compiled code */ }

    init() { /* compiled code */ }
}

let _FRAMEWORK: std.unittest.mock.MockFramework

public class MockFramework {
    private let sessions: std.collection.ArrayList<std.unittest.mock.MockSession>

    private var recording: std.core.Option<std.unittest.mock.RecordingHandler>

    private let globalLock: std.sync.ReentrantMutex

    private let objCounter: std.sync.AtomicUInt64

    init() { /* compiled code */ }

    public static func openSession(name: std.core.String, sessionKind: std.unittest.mock.MockSessionKind): Unit { /* compiled code */ }

    public static func closeSession(): Unit { /* compiled code */ }

    internal func generateUniqueId(): UInt64 { /* compiled code */ }

    internal static func session<T>(body: (std.unittest.mock.MockSession) -> T): T { /* compiled code */ }

    internal static func recordCall<R>(recordingInfo: std.unittest.mock.RecordingInfo, dummyCall: () -> R): std.unittest.mock.RecordedCall { /* compiled code */ }

    internal func handleSync(call: std.unittest.mock.internal.Call, objId: UInt64, objectCreatedAtSessionName: std.core.Option<std.core.String>): std.unittest.mock.internal.OnCall { /* compiled code */ }

    internal func registerObject(mockObject: std.unittest.mock.MockObject): Unit { /* compiled code */ }

    internal func findObjectIdsByRefs(referencesToFind: std.core.Array<std.core.Object>): std.collection.Set<UInt64> { /* compiled code */ }

    protected func throwNotAMockObject(): Nothing { /* compiled code */ }
}

internal type UnmatchedChains = std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>

internal enum MatchStatus {
    | Ok

    | FuncMismatch

    | ReceiverMismatch

    | ArgumentMismatch(std.core.Array<Int64>)

    | TypeArgMismatch(std.core.Array<Int64>)
}

internal class RecordingHandler {
    private var data: std.core.Option<std.unittest.mock.RecordedCall>

    internal func record(call: std.unittest.mock.internal.Call, object: std.core.Option<std.unittest.mock.MockObject>): Unit { /* compiled code */ }

    internal func getRecorded(): std.unittest.mock.RecordedCall { /* compiled code */ }

    internal let info: std.unittest.mock.RecordingInfo

    RecordingHandler(info: std.unittest.mock.RecordingInfo) { /* compiled code */ }
}

public enum MockSessionKind {
    | Forbidden

    | Stateless

    | Verifiable
}

internal struct RecordingInfo {
    internal let target: std.unittest.mock.RecordingTarget

    internal let prefixRefName: std.core.Option<std.core.String>

    internal let declarationName: std.core.String

    RecordingInfo(target: std.unittest.mock.RecordingTarget, prefixRefName: std.core.Option<std.core.String>, declarationName: std.core.String) { /* compiled code */ }
}

internal struct RecordedCall {
    internal let object: std.core.Option<std.unittest.mock.MockObject>

    internal let stubId: std.unittest.mock.StubId

    internal let location: std.unittest.mock.Location

    internal let typeArgs: std.core.Array<std.core.String>

    RecordedCall(object: std.core.Option<std.unittest.mock.MockObject>, stubId: std.unittest.mock.StubId, location: std.unittest.mock.Location, typeArgs: std.core.Array<std.core.String>) { /* compiled code */ }
}

extend std.unittest.mock.MockObject {
    internal func hasMode(mode: std.unittest.mock.StubMode): Bool { /* compiled code */ }
}

extend std.unittest.mock.Invocation {
    internal prop isDefaultEnabled: Bool {
        get() { /* compiled code */ }
    }

    internal prop canReturnDefaultValue: Bool {
        get() { /* compiled code */ }
    }
}

internal class MockScopeViolation <: std.unittest.mock.FailureReport {
    internal static let HINT: std.core.String

    internal func reportInaccessible(createdAtSessionName: std.core.Option<std.core.String>, whereSessionName: std.core.Option<std.core.String>): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

internal enum SessionHandleResult {
    | PerformScenario(std.unittest.mock.Scenario)

    | Unhandled

    | InvocationLimitExceeded(std.unittest.mock.StubChain)
}

extend std.unittest.mock.MockSession {
    internal func handle(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>): std.unittest.mock.SessionHandleResult { /* compiled code */ }
}

extend std.unittest.mock.StubChain {
    internal func runListeners(invocation: std.unittest.mock.Invocation): Unit { /* compiled code */ }
}

internal enum InternalError {
    | InconsistentState(std.core.String)

    | AssertionFailed(std.core.String)

    | Unimplemented(std.core.String)
}

internal func internalError(e: std.unittest.mock.InternalError): Nothing { /* compiled code */ }

internal func internalError(assertion: std.core.String): Nothing { /* compiled code */ }

internal func illegalInput(text: std.core.String): Nothing { /* compiled code */ }

internal enum BehaviourSource {
    | Stub(std.unittest.mock.StubChain)

    | Default

    | Failure(std.unittest.mock.PrettyException)
}

internal class MutableLog {
    private let entries: std.collection.ArrayList<std.unittest.mock.LogEntry>

    private var lastClearIndex: Int64

    internal func newEntry(entry: std.unittest.mock.LogEntry): Unit { /* compiled code */ }

    internal func clear(): Unit { /* compiled code */ }

    internal func snapshotSinceLastClear(scope: std.collection.Set<UInt64>): std.unittest.mock.LogSnapshot { /* compiled code */ }

    internal func fullLog(): std.unittest.mock.LogSnapshot { /* compiled code */ }

    internal func validateLog(): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

internal class LogCorruptedReport <: std.unittest.mock.FailureReport {
    internal func fail(originalMessage: std.unittest.common.PrettyPrintable): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

internal struct TraceEntry {
    internal let filePath: std.core.String

    internal let lineNumber: Int64

    TraceEntry(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let UNKNOWN_LOCATION: std.unittest.mock.LocationImpl

internal class LocationImpl <: std.unittest.mock.Location {
    public func getEntries(): std.core.Array<std.unittest.mock.TraceEntry> { /* compiled code */ }

    private let entries: std.core.Array<std.unittest.mock.TraceEntry>

    LocationImpl(entries: std.core.Array<std.unittest.mock.TraceEntry>) { /* compiled code */ }
}

let UNKNOWN_LOCATION_STRING: std.core.String

internal class SimpleLocation <: std.unittest.mock.Location {
    public func getEntries(): std.core.Array<std.unittest.mock.TraceEntry> { /* compiled code */ }

    internal let filePath: std.core.String

    internal let lineNumber: Int64

    SimpleLocation(filePath: std.core.String, lineNumber: Int64) { /* compiled code */ }
}

let MOCKED_CLASS_NAME_FRAGMENT: std.core.String

internal func locationFromTrace(): std.unittest.mock.Location { /* compiled code */ }

extend std.unittest.mock.Location {
    internal func forceLineNumber(lineNum: Int64): std.unittest.mock.Location { /* compiled code */ }

    internal func fileNameAndLine(): std.core.String { /* compiled code */ }
}

public class NoneMatcher <: std.unittest.mock.ArgumentMatcher {
    public override func matchesAny(arg: std.core.Any): Bool { /* compiled code */ }

    NoneMatcher() { /* compiled code */ }
}

extend std.unittest.mock.Matchers {
    public static func none(): std.unittest.mock.NoneMatcher { /* compiled code */ }
}

extend std.unittest.mock.NoneMatcher {
    public func value<T>(): std.core.Option<T> { /* compiled code */ }
}

internal enum RecordingTarget {
    | MethodStub

    | VerifyStatementStub
}

internal enum RecordingFailureReason {
    | NotAMock

    | UnmockableCallable
}

internal class RecordingFailureReport <: std.unittest.mock.FailureReport {
    internal func fail(info: std.unittest.mock.RecordingInfo, possibleReasons: std.core.Array<std.unittest.mock.RecordingFailureReason>): Nothing { /* compiled code */ }

    init() { /* compiled code */ }
}

internal enum RuntimeFailureReason {
    | UnstubbedDeclaration(std.unittest.mock.internal.FuncInfo)

    | NoneMatchedArguments(std.core.Array<(std.unittest.mock.StubChain, std.core.Array<Int64>)>)

    | TooManyInvocationsForStubChain(std.unittest.mock.StubChain)

    | UnwantedInteraction(std.unittest.mock.StubChain)
}

internal func unhandledInvocationReport(invocation: std.unittest.mock.Invocation, unmatchedChains: std.collection.ArrayList<(std.unittest.mock.StubChain, std.unittest.mock.MatchStatus)>, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

internal func invocationLimitExceededReport(currentInvocation: std.unittest.mock.Invocation, chain: std.unittest.mock.StubChain, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

internal func runtimeUnwantedInteraction(currentInvocation: std.unittest.mock.Invocation, sc: std.unittest.mock.Scenario, invocationLog: std.unittest.mock.LogSnapshot): std.unittest.mock.PrettyException { /* compiled code */ }

internal class RuntimeFailureReport <: std.unittest.mock.FailureReport {
    internal func render(invocation: std.unittest.mock.Invocation, reason: std.unittest.mock.RuntimeFailureReason): std.unittest.common.PrettyText { /* compiled code */ }

    internal func findInvocationsHandledByChain(stubChain: std.unittest.mock.StubChain): std.core.Array<std.unittest.mock.Invocation> { /* compiled code */ }

    RuntimeFailureReport(invocationLog: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

internal enum Action {
    | Return(() -> std.core.Any)

    | Throw(() -> std.core.Exception)

    | CallOriginal

    | Fail

    | GetField(std.unittest.mock.SyntheticFieldDescription)

    | SetField(std.unittest.mock.SyntheticFieldDescription)
}

internal struct ScenarioConfiguration {
    internal let action: std.unittest.mock.Action

    internal let cardinality: std.unittest.mock.Cardinality

    ScenarioConfiguration(action: std.unittest.mock.Action, cardinality: std.unittest.mock.Cardinality) { /* compiled code */ }
}

internal enum ScenarioStatus {
    | Created(std.core.Option<std.unittest.mock.Action>, std.core.Option<std.unittest.mock.Cardinality>)

    | Active(std.unittest.mock.ScenarioConfiguration, std.core.Box<Int64>)
}

internal interface UntypedOption {
    public open func value(): std.core.Option<std.core.Any>
}

extend std.core.Option<T> <: UntypedOption {
    public func value(): std.core.Option<std.core.Any> { /* compiled code */ }
}

internal func unwrapValue<T>(value: std.core.Any): std.core.Option<T> { /* compiled code */ }

public enum Exhaustiveness {
    | Exhaustive

    | Partial
}

internal enum Orderedness {
    | Ordered

    | Unordered
}

internal enum VerificationResult {
    | Success

    | Failure(std.unittest.mock.VerificationFailedReason)
}

internal enum VerificationFailedReason {
    | TooFewInvocations(std.unittest.mock.VerifyStatement, std.unittest.mock.Cardinality, Int64)

    | TooManyInvocations(std.unittest.mock.VerifyStatement, std.unittest.mock.Cardinality, Int64)

    | UnmatchedStatements(std.core.Array<std.unittest.mock.VerifyStatement>)

    | UnmatchedInvocations(std.core.Array<std.unittest.mock.Invocation>)

    | UnexpectedInvocation(std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)

    | NonDisjointStatements(std.unittest.mock.VerifyStatement, std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)

    | UnwantedInteractions(std.core.Array<std.unittest.mock.Invocation>)
}

internal func performVerification(statements: std.core.Array<std.unittest.mock.VerifyStatement>, orderedness: std.unittest.mock.Orderedness, exhaustiveness: std.unittest.mock.Exhaustiveness): Unit { /* compiled code */ }

internal func verifyNoInteractions(objectRefs: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

internal func ensureDistinctReferences(refs: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

internal func collectObjectsIds(statements: std.core.Array<std.unittest.mock.VerifyStatement>): std.collection.HashSet<UInt64> { /* compiled code */ }

internal func checkMatchersConsistency(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

internal class VerificationFailureReport <: std.unittest.mock.FailureReport {
    internal func fail(reason: std.unittest.mock.VerificationFailedReason): Nothing { /* compiled code */ }

    VerificationFailureReport(logSnapshot: std.unittest.mock.LogSnapshot, nameRegistry: std.unittest.mock.MockNameRegistry) { /* compiled code */ }
}

public class Verify {
    public static func that(statement: std.unittest.mock.VerifyStatement): Unit { /* compiled code */ }

    public static func unordered(exhaustive: std.unittest.mock.Exhaustiveness, collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit { /* compiled code */ }

    public static func unordered(collectStatements: (std.unittest.mock.UnorderedVerifier) -> Unit): Unit { /* compiled code */ }

    public static func unordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    public static func unordered(exhaustive: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    public static func ordered(collectStatements: (std.unittest.mock.OrderedVerifier) -> Unit): Unit { /* compiled code */ }

    public static func ordered(statements: std.core.Array<std.unittest.mock.VerifyStatement>): Unit { /* compiled code */ }

    public static func clearInvocationLog(): Unit { /* compiled code */ }

    public static func noInteractions(mockObjects: std.core.Array<std.core.Object>): Unit { /* compiled code */ }

    init() { /* compiled code */ }
}

public class UnorderedVerifier {
    internal let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    public func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.UnorderedVerifier { /* compiled code */ }

    UnorderedVerifier() { /* compiled code */ }
}

public class OrderedVerifier {
    internal let statements: std.collection.ArrayList<std.unittest.mock.VerifyStatement>

    public func checkThat(statement: std.unittest.mock.VerifyStatement): std.unittest.mock.OrderedVerifier { /* compiled code */ }

    OrderedVerifier() { /* compiled code */ }
}

internal class VerifyIterator {
    internal let statementIterator: std.core.Iterator<std.unittest.mock.VerifyStatement>

    internal var currentStatement: std.unittest.mock.VerifyStatement

    internal var nextStatement: std.core.Option<std.unittest.mock.VerifyStatement>

    internal var logIndex: Int64

    internal var invocation: std.unittest.mock.Invocation

    internal var currentStatementMatch: Bool

    internal var nextStatementMatch: Bool

    internal func goToNextStatement(): Bool { /* compiled code */ }

    internal func hasMoreStatements(): Bool { /* compiled code */ }

    internal func goToNextInvocation(): Bool { /* compiled code */ }

    internal func check(count: Int64): std.unittest.mock.CardinalityCheck { /* compiled code */ }

    internal let statements: std.core.Iterable<std.unittest.mock.VerifyStatement>

    internal let invocations: std.core.Array<std.unittest.mock.Invocation>

    VerifyIterator(statements: std.core.Iterable<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>) { /* compiled code */ }
}

internal func verifyExhaustiveOrderedIteration(iterator: std.unittest.mock.VerifyIterator): std.core.Option<std.unittest.mock.VerificationResult> { /* compiled code */ }

internal func verifyExhaustiveOrdered(statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult { /* compiled code */ }

extend std.unittest.mock.VerifyIterator {
    internal func tooFewInvocations(count: Int64): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func tooManyInvocations(count: Int64): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func nonDisjointStatements(): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func unmatchedStatements(): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func unmatchedInvocations(): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func unexpectedInvocation(): std.unittest.mock.VerificationResult { /* compiled code */ }

    internal func leftoverStatements(): std.core.Array<std.unittest.mock.VerifyStatement> { /* compiled code */ }
}

internal func verifyUnordered(exhastiveness: std.unittest.mock.Exhaustiveness, statements: std.core.Array<std.unittest.mock.VerifyStatement>, log: std.unittest.mock.LogSnapshot): std.unittest.mock.VerificationResult { /* compiled code */ }

internal func batchMatch(statements: std.core.Array<std.unittest.mock.VerifyStatement>, invocations: std.core.Array<std.unittest.mock.Invocation>): std.unittest.mock.BatchMatchResult { /* compiled code */ }

internal enum BatchMatchResult {
    | DisjointMatching(std.core.Array<std.core.Option<std.unittest.mock.VerifyStatement>>, std.core.Array<Int64>)

    | Ambiguity(std.unittest.mock.VerifyStatement, std.unittest.mock.VerifyStatement, std.unittest.mock.Invocation)
}
