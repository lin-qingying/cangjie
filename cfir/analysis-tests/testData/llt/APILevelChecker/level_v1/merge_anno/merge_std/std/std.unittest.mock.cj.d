// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.




package std.unittest.mock
import std.collection.*
import std.fs.*
import std.sync.*
import std.unittest.common.*
import std.unittest.mock.internal.*


/**
* Part of the stub chain. Specifies an action to be performed by the stub.
*/
@!APILevel[12, atomicservice : true]
public sealed abstract class ActionSelector {
    /**
    * Triggering this stub will fail the test.
    */
    @!APILevel[12, atomicservice : true]
    public func fails(): Unit
}

/**
* Defines actions available for methods.
*/
@!APILevel[12, atomicservice : true]
public class MethodActionSelector<TRet> <: ActionSelector {
    /**
    * Stub will throw an exception.
    * @param exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exception: Exception): CardinalitySelector<MethodActionSelector<TRet>>
    
    /**
    * Stub will throw an exception produced by the factory.
    * @param exceptionFactory will be called on each invocation to produce an exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exceptionFactory: () -> Exception): CardinalitySelector<MethodActionSelector<TRet>>
    
    /**
    * Stub will return a given value.
    * @param value to be returned.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func returns(value: TRet): CardinalitySelector<MethodActionSelector<TRet>>
    
    /**
    * Stub will return a value produced by the factory.
    * @param valueFactory will be called on each invocation to produce return value.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func returns(valueFactory: () -> TRet): CardinalitySelector<MethodActionSelector<TRet>>
    
    /**
    * Stub will return given values consecutively.
    * Stub will be expected to be called exactly values.size times.
    * @param values an array of values to be returned one after another.
    * @return Continuation for that action that will be invoked after all values in the array were returned.
    */
    @!APILevel[12, atomicservice : true]
    public func returnsConsecutively(values: Array<TRet>): Continuation<MethodActionSelector<TRet>>
    
    /**
    * Stub will return given values consecutively.
    * Stub will be expected to be called exactly values.size times.
    * @param values a list of values to be returned one after another.
    * @return Continuation for that action that will be invoked after all values in the list were returned.
    */
    @!APILevel[12, atomicservice : true]
    public func returnsConsecutively(values: ArrayList<TRet>): Continuation<MethodActionSelector<TRet>>
    
    /**
    * Stub will invoke method on original object.
    * Only available for spy objects.
    * @return CardinalitySelector for that action.
    * @throws MockFrameworkException if an object being stubbed is not a spy object.
    */
    @!APILevel[12, atomicservice : true]
    public func callsOriginal(): CardinalitySelector<MethodActionSelector<TRet>>
}

/**
* Defines actions available for field read operations or property getters.
*/
@!APILevel[12, atomicservice : true]
public class GetterActionSelector<TRet> <: ActionSelector {
    /**
    * Stub will throw an exception.
    * @param exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exception: Exception): CardinalitySelector<GetterActionSelector<TRet>>
    
    /**
    * Stub will throw an exception produced by the factory.
    * @param exceptionFactory will be called on each invocation to produce an exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exceptionFactory: () -> Exception): CardinalitySelector<GetterActionSelector<TRet>>
    
    /**
    * Stub will return a given value.
    * @param value to be returned.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func returns(value: TRet): CardinalitySelector<GetterActionSelector<TRet>>
    
    /**
    * Stub will return a value produced by the factory.
    * @param valueFactory will be called on each invocation to produce return value.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func returns(valueFactory: () -> TRet): CardinalitySelector<GetterActionSelector<TRet>>
    
    /**
    * Stub will return given values consecutively.
    * Stub will be expected to be called exactly values.size times.
    * @param values an array of values to be returned one after another.
    * @return Continuation for that action that will be invoked after all values in the array were returned.
    */
    @!APILevel[12, atomicservice : true]
    public func returnsConsecutively(values: Array<TRet>): Continuation<GetterActionSelector<TRet>>
    
    /**
    * Stub will return given values consecutively.
    * Stub will be expected to be called exactly values.size times.
    * @param values a list of values to be returned one after another.
    * @return Continuation for that action that will be invoked after all values in the list were returned.
    */
    @!APILevel[12, atomicservice : true]
    public func returnsConsecutively(values: ArrayList<TRet>): Continuation<GetterActionSelector<TRet>>
    
    /**
    * Stub will get original field or call original property getter.
    * Only available for spy objects.
    * @return CardinalitySelector for that action.
    * @throws MockFrameworkException if an object being stubbed is not a spy object.
    */
    @!APILevel[12, atomicservice : true]
    public func getsOriginal(): CardinalitySelector<GetterActionSelector<TRet>>
    
    /**
    * Stub will get value of synthetic field.
    * @param field synthetic field object managed by the framework.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func getsField(field: SyntheticField<TRet>): CardinalitySelector<GetterActionSelector<TRet>>
}

/**
* Defines actions available for field write operations or property setters.
*/
@!APILevel[12, atomicservice : true]
public class SetterActionSelector<TArg> <: ActionSelector {
    /**
    * Stub will throw an exception.
    * @param exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exception: Exception): CardinalitySelector<SetterActionSelector<TArg>>
    
    /**
    * Stub will throw an exception produced by the factory.
    * @param exceptionFactory will be called on each invocation to produce an exception to be thrown.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func throws(exceptionFactory: () -> Exception): CardinalitySelector<SetterActionSelector<TArg>>
    
    /**
    * Stub will do nothing (return () ).
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func doesNothing(): CardinalitySelector<SetterActionSelector<TArg>>
    
    /**
    * Stub will set original field or call original property setter.
    * Only available for spy objects.
    * @return CardinalitySelector for that action.
    * @throws MockFrameworkException if an object being stubbed is not a spy object.
    */
    @!APILevel[12, atomicservice : true]
    public func setsOriginal(): CardinalitySelector<SetterActionSelector<TArg>>
    
    /**
    * Stub will set value of synthetic field.
    * @param field synthetic field object managed by the framework.
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func setsField(field: SyntheticField<TArg>): CardinalitySelector<SetterActionSelector<TArg>>
}

extend MethodActionSelector<Unit> {
    /**
    * Stub will do nothing (return () ).
    * @return CardinalitySelector for that action.
    */
    @!APILevel[12, atomicservice : true]
    public func returns(): CardinalitySelector<MethodActionSelector<Unit>>
}

/**
* ArgumentMatcher represent an expression that can be used
*     instead of an argument inside @On or @Called macro.
* Instances of this class should be obtained using static methods of Matchers class.
*/
@!APILevel[12, atomicservice : true]
public abstract class ArgumentMatcher {
    /**
    * @param description to be used in error reports.
    * @returns receiver.
    */
    @!APILevel[12, atomicservice : true]
    public func withDescription(description: String): ArgumentMatcher
    
    /**
    * @param name of the parameter that this matcher is for.
    * @returns receiver.
    */
    @!APILevel[12, atomicservice : true]
    public func forParameter(name: String): ArgumentMatcher
    
    /**
    * @param arg value of the argument.
    * @returns whether argument value should be accpeted or not.
    */
    @!APILevel[12, atomicservice : true]
    public func matchesAny(arg: Any): Bool
}

/**
* Base interface for typed matchers: matchers that only match values of certain types.
*/
@!APILevel[12, atomicservice : true]
public abstract class TypedMatcher<T> <: ArgumentMatcher {
    /**
    * @param arg value of the argument.
    * @returns whether argument value should be accepted or not.
    */
    @!APILevel[12, atomicservice : true]
    public func matches(arg: T): Bool
    
    @!APILevel[12, atomicservice : true]
    public override func matchesAny(arg: Any): Bool
}

/**
* Matcher that matches any values.
*/
@!APILevel[12, atomicservice : true]
public class AnyMatcher <: ArgumentMatcher {
    @!APILevel[12, atomicservice : true]
    public func matchesAny(_: Any): Bool
}

/**
* Contains definitions for argument matchers that can be used as part of '@On(<stub call>)' stubbing chain.
* Argument matchers must be defined as static functions on Matchers class.
*/
@!APILevel[12, atomicservice : true]
public class Matchers {
    /**
    * Matches arguments based on provided predicate.
    * @param predicate used to filter values that are accepted by the predicate.
    * @returns argument matcher that accepts values that satisfy 'predicate(value) == true' condition.
    */
    @!APILevel[12, atomicservice : true]
    public static func argThat<T>(predicate: (T) -> Bool): TypedMatcher<T>
    
    /**
    * Matches arguments based on provided predicate.
    * @param predicate used to filter out values that are rejected.
    * @returns argument matcher that accepts values that satisfy 'predicate(value) == false' condition.
    */
    @!APILevel[12, atomicservice : true]
    public static func argThatNot<T>(predicate: (T) -> Bool): TypedMatcher<T>
    
    /**
    * Matches arguments based on provided predicate.
    * Allows listening to passed in values.
    * Note that only values accepted by the predicate can be listened to.
    *
    * @param listener that will be invoked on each matching (accepted by the predicate) value passed to the stub.
    * @param predicate used to filter values that are accepted.
    * @returns argument matcher that accepts values that satisfy 'predicate(value) == true' condition.
    */
    @!APILevel[12, atomicservice : true]
    public static func argThat<T>(listener: ValueListener<T>, predicate: (T) -> Bool): TypedMatcher<T>
    
    /**
    * Filters input value based on reference equality to the provided object.
    *
    * @param target that must be matched.
    * @returns argument matcher only allowing references to the given object.
    */
    @!APILevel[12, atomicservice : true]
    public static func same<T>(target: T): TypedMatcher<T> where T <: Object
    
    /**
    * Filters input value based on structural equality to the provided value.
    *
    * @param target that must be matched.
    * @returns argument matcher only allowing values structurally equal to the given value.
    */
    @!APILevel[12, atomicservice : true]
    public static func eq<T>(target: T): TypedMatcher<T> where T <: Equatable<T>
    
    /**
    * Filters input value based on their type.
    *
    * @returns argument matcher only allowing values of certain type.
    */
    @!APILevel[12, atomicservice : true]
    public static func ofType<T>(): TypedMatcher<T>
    
    /**
    * Allows any value to be passed in as an argument to the stub.
    *
    * @returns argument matcher allowing any value.
    */
    @!APILevel[12, atomicservice : true]
    public static func any(): AnyMatcher
    
    /**
    * Allows listening to any value of type T passed in as an argument.
    * If the type argument is not specified explicitly then it will be inferred from the type argument of the listener.
    *
    * @param listener that will be invoked on each value passed in as an argument to the stub.
    * @returns argument matcher allowing any value.
    */
    @!APILevel[12, atomicservice : true]
    public static func capture<T>(listener: ValueListener<T>): TypedMatcher<T>
    
    /**
    * Default matcher will match values based on structural (higher priority) or reference equality.
    * Appicabilty of this matcher is not guaranteed at compile time.
    * If passed in argument is neither Equatable<T> nor a reference type an exception will be thrown at runtime.
    *
    * @param target that must be matched either by structural or reference equality.
    * @returns argument matcher allowing values matching target.
    * @throws MockFrameworkException if target is neither equatable not a reference type.
    */
    @!APILevel[12, atomicservice : true]
    public static func default<T>(target: T): TypedMatcher<T>
}

/**
* These methods are used during macro expansion to enforce compiler error on type mismatch.
* Do not use directly.
*/
extend<T> TypedMatcher<T> {
    @!APILevel[12, atomicservice : true]
    public func value(): T
}

extend AnyMatcher {
    @!APILevel[12, atomicservice : true]
    public func value<T>(): T
}

/**
* Allows listening to arguments passed in to stubs.
* Can be used together with *argThat* or *capture* argument matchers.
*/
@!APILevel[12, atomicservice : true]
public interface ValueListener<T> {
}

@!APILevel[12, atomicservice : true]
public class ConfigureMock {
    @!APILevel[12, atomicservice : true]
    public static func stubFunction<TRet>(
        stubCall: () -> TRet,
        matchers: Array<ArgumentMatcher>,
        prefixRefName: Option<String>,
        methodName: String,
        callDescription: String,
        lineNumber: Int64
    ): MethodActionSelector<TRet>
    
    @!APILevel[12, atomicservice : true]
    public static func stubGetter<TRet>(
        stubCall: () -> TRet,
        prefixRefName: Option<String>,
        fieldOrPropertyName: String,
        callDescription: String,
        lineNumber: Int64
    ): GetterActionSelector<TRet>
    
    @!APILevel[12, atomicservice : true]
    public static func stubSetter<TArg>(
        stubCall: () -> Unit,
        _: () -> TArg, // capturing type of property/field
        matcher: ArgumentMatcher,
        prefixRefName: Option<String>,
        fieldOrPropertyName: String,
        callDescription: String,
        lineNumber: Int64
    ): SetterActionSelector<TArg>
}

/*
* Specifies additional default behaviour for mock objects.
* Explicit stubs always override default behaviour.
*/
@!APILevel[12, atomicservice : true]
public enum StubMode {
    @!APILevel[12, atomicservice : true]
    /**
    * Mock object will return default values for common 'simple' types.
    * Use to simplify mock object setup.
    * These values are 'negative' or 'empty'
    * Supported types are: Unit, number types, option types, Bool, String, Array, ArrayList, HashSet, HashMap.
    */
    ReturnsDefaults |
    @!APILevel[12, atomicservice : true]
    /**
    * Mock object will treat its mutable properties and fields as if they are mutable fields.
    * Similar to using SyntheticField directly but less verbose. 
    * Reading an unitialized field will result in an error.
    */
    SyntheticFields
}

/**
* Creates a mock object of type T. This object does not have any behaviour specified for its members by default.
* Behaviour for mock object can be specified using @On macro.
*
* @returns mock object of type T.
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func mock<T>(): T

/**
* Creates a mock object of type T. This object does not have any behaviour specified for its members by default.
* Behaviour for mock object can be specified using @On macro.
*
* @param modes - stub modes altering mock object default behaviour.
* @returns mock object of type T.
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func mock<T>(modes: Array<StubMode>): T

/**
* Creates a spy object of type T. Spy object wraps a passed in instance and delegates all calls to that instance by default.
* Behaviour for members of the spy object can be overridden using @On macro.
*
* @param objectToSpyOn instance to spy on.
* @returns spy object that spies on passed in instance of type T.
*/
@Frozen
@!APILevel[12, atomicservice : true]
public func spy<T>(objectToSpyOn: T): T

@!APILevel[12, atomicservice : true]
public abstract class PrettyException <: Exception & PrettyPrintable {
    @!APILevel[12, atomicservice : true]
    public func pprint(to: PrettyPrinter): PrettyPrinter
}

/**
* An error in mock framework implementation. User should not expect this exception to be thrown.
*/
@!APILevel[12, atomicservice : true]
public class MockFrameworkInternalError <: PrettyException {
}

/**
* Erroneous or inconsistent use of mock framework API.
*/
@!APILevel[12, atomicservice : true]
public class MockFrameworkException <: PrettyException {
}

/**
* One or more expectation set during mock configuration were violated during test execution.
*/
@!APILevel[12, atomicservice : true]
public open class ExpectationFailedException <: PrettyException {
}

/**
* Indicates that a stub was never triggered by the code under test.
*/
@!APILevel[12, atomicservice : true]
public class UnnecessaryStubbingException <: PrettyException {
}

/**
* No stubs that match this invocation were provided.
*/
@!APILevel[12, atomicservice : true]
public class UnstubbedInvocationException <: PrettyException {
}

/**
* None of the provided stubs handled the call.
*/
@!APILevel[12, atomicservice : true]
public class UnhandledCallException <: PrettyException {
}

@!APILevel[12, atomicservice : true]
public class VerificationFailedException <: PrettyException {
}

@!APILevel[12, atomicservice : true]
public class SyntheticField<T> {
    @!APILevel[12, atomicservice : true]
    public static func create(initialValue!: T): SyntheticField<T>
}

@!APILevel[12, atomicservice : true]

@!APILevel[12, atomicservice : true]
public class MockFramework {
    /**
    * Opens a new session. Sessions form a stacklike structure.
    * Sessions are closed in the reverse order they were opened in.
    * Mock objects created during a given session are only accessible inside the session or any of it's inner sessions.
    * Each session keeps its own invocation log so any verification is performed on calls made inside latest open session.
    * Expectations can only be verified whenever the session is closed.
    *
    * @param name to identify a session for debugging purposes.
    * @param sessionKind indicating what stubs are allowed in this session.
    */
    @!APILevel[12, atomicservice : true]
    public static func openSession(name: String, sessionKind: MockSessionKind): Unit
    
    /**
    * Closes the session. Sessions can only be closed in the same order they were opened in.
    *
    * Checks for misconfiguration errors such as stubs created but not configured.
    * Automatically verifies expectations for all declared stubs iff the session was *Verifiable*. See *MockSessionKind*.
    *
    * @throws MockFrameworkException if any misconfiguration errors were detected.
    * @throws ExpectationFailedException if any of the expectations were not met.
    */
    @!APILevel[12, atomicservice : true]
    public static func closeSession(): Unit
}

/**
* Controls what kind of stubs are allowed inside a given *MockSession*.
* Forbidden - no stubs allowed.
* Stateless - only stateless are allowed (inherently stateful actions like returnsConsequitively and cardinality specifiers are disallowed).
* Verifiable - any stub is allowed.
*
* Expectations can only be verified for stubs created in *Verifiable* sessions.
*/
@!APILevel[12, atomicservice : true]
public enum MockSessionKind {
    @!APILevel[12, atomicservice : true]
    Forbidden |
    @!APILevel[12, atomicservice : true]
    Stateless |
    @!APILevel[12, atomicservice : true]
    Verifiable
}

@!APILevel[12, atomicservice : true]
public class NoneMatcher <: ArgumentMatcher {
    @!APILevel[12, atomicservice : true]
    public override func matchesAny(arg: Any): Bool
}

extend Matchers {
    @!APILevel[12, atomicservice : true]
    public static func none(): NoneMatcher
}

extend NoneMatcher {
    @!APILevel[12, atomicservice : true]
    public func value<T>(): Option<T>
}

/**
* Part of stub chain. Specifies cardinality of an action that comes immediately before it in the stub chain.
*
* Cardinality can imply verification to be done by the framework:
*     Stubs invoked more times than specified will throw ExpectationFailedException immediately.
*     For stubs that were not invoked enough times framework will throw ExpectationFailedException after code under test finished executing.
*/
@!APILevel[12, atomicservice : true]
public class CardinalitySelector<A> where A <: ActionSelector {
    /**
    * Specifies that the stub can be invoked any number of times.
    * There are no expectations for this stub.
    */
    @!APILevel[12, atomicservice : true]
    public func anyTimes(): Unit
    
    /**
    * Specifies that the stub must be invoked exactly one time.
    * @return continuation representing additional actions to be performed when conditions were satisfied.
    */
    @!APILevel[12, atomicservice : true]
    public func once(): Continuation<A>
    
    /**
    * Specifies that the stub must be invoked at least one time.
    */
    @!APILevel[12, atomicservice : true]
    public func atLeastOnce(): Unit
    
    /**
    * Specifies that the stub must be invoked an exact number of times.
    * @param expectedTimes number of times stub must be invoked.
    * @return continuation representing additional actions to be performed when conditions were satisfied.
    */
    @!APILevel[12, atomicservice : true]
    public func times(expectedTimes: Int64): Continuation<A>
    
    /**
    * Specifies that the stub must be invoked [min..max] times.
    * @param min minimum number of times the stub must be invoked.
    * @param max maximum number of times the stub must be invoked.
    * @return continuation representing additional actions to be performed when conditions were satisfied.
    */
    @!APILevel[12, atomicservice : true]
    public func times(min!: Int64, max!: Int64): Unit
    
    /**
    * Specifies that the stub must be invoked at least certain number of times.
    * @param minTimesExpected minimum number of times the stub must be called.
    */
    @!APILevel[12, atomicservice : true]
    public func atLeastTimes(minTimesExpected: Int64): Unit
}

/**
* Part of the stub chain. Allows to specify additional action will be performed
*     by the stub when previous actions cardinality will be satisfied.
* Specifying continuation only makes sense if it is followed by an action.
* MockFrameworkException will be thrown if there are any unfinished stub chains.
* No guarantees are given on exact point this exception will be thrown.
*/
@!APILevel[12, atomicservice : true]
public class Continuation<A> where A <: ActionSelector {
    /**
    * @return an ActionSelector for an action to be performed when previous actions in the chain are completed.
    */
    @!APILevel[12, atomicservice : true]
    public func then(): A
}

extend<T> Option<T> <: UntypedOption {
    @!APILevel[12, atomicservice : true]
    public func value(): ?Any
}

/**
* Represents the exhaustiveness of verification.
* Exhaustive - requires every invocation on an object to be listed inside Verify block.
* Partial - allows to list only some invocations and ignore 'uninteresting' invoactions.
*/
@!APILevel[12, atomicservice : true]
public enum Exhaustiveness {
    @!APILevel[12, atomicservice : true]
    Exhaustive |
    @!APILevel[12, atomicservice : true]
    Partial
}

/**
* Entry point for Verification API.
* Allows perform checks on invocation log that is tracked for mock and spy objects.
* Invocation log is kept since the start of a @TestCase function and is cleared after.
* Calling verification functions (*that*, *ordered*, *unordered*) will not change the state of invocation log.
* To clear the log manually use *clearInvocationLog* function.
*/
@!APILevel[12, atomicservice : true]
public class Verify {
    /**
    * Verifies that a single *VerifyStatement* is true. Calls not matching this statement will be ignored.
    * @param VerifyStatement to check.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func that(
        statement: VerifyStatement
    ): Unit
    
    /**
    * Performs *unordered* verification.
    * Checks that invocations match statements irrespective of the order statements were declared in.
    * *VerifyStatement*s are provided via *collectStatements* callback.
    * Statements in *unordered* section must be disjoint (effectively a single invocation matching several statements will be considered an error).
    * *atLeastOnce* cardinality is assumed for *VerifyStatements* if not specified explicitly.
    * A single invocation matching several *VerifyStatement*s is considered a failure.
    * Exhaustiveness of verification is controlled by *exhaustive* parameter.
    * Partial verification allows uninteresting function calls to be omitted: only calls matching *VerifyStatement*s provided in *collectStatements* block will be considered.
    * Exhaustive verification requires that any invocation on any of mock objects mentioned in the *collectStatements* block is listed in the block.
    *
    * @param exhaustive if verification should be exhaustive.
    * @param collectStatement callback allowing to collect verification statements for this verification session.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func unordered(
        exhaustive: Exhaustiveness,
        collectStatements: (UnorderedVerifier) -> Unit
    ): Unit
    
    /**
    * Performs *unordered* exhaustive verification.
    *
    * @param collectStatement callback allowing to collect verification statements for this verification session.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func unordered(
        collectStatements: (UnorderedVerifier) -> Unit
    ): Unit
    
    /**
    * Performs *unordered* exhaustive verification.
    *
    * @param exhaustive if verification should be exhaustive.
    * @param statements to be verified.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func unordered(statements: Array<VerifyStatement>): Unit
    
    /**
    * Performs *unordered* exhaustive verification.
    *
    * @param statements to be verified.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func unordered(
        exhaustive: Exhaustiveness,
        statements: Array<VerifyStatement>
    ): Unit
    
    /**
    * Performs *ordered* verification.
    * Checks that invocations match statements in the same exact order that statements were declared in.
    * *VerifyStatement*s are provided via *collectStatements* callback.
    * Consequitve statements in *ordered* section must be dijsoint. An invocation matching several consequitve statements will be considered an error.
    * *once* cardinality is assumed for *VerifyStatements* if not specified explicitly.
    * Ordered verification is always exhaustive: all invocations on mock object mention in *ordered* section must be listed.
    *
    * @param collectStatement callback allowing to collect verification statements for this verification session.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func ordered(
        collectStatements: (OrderedVerifier) -> Unit
    ): Unit
    
    /**
    * Performs *ordered* verification.
    *
    * @param statements to be verified.
    * @throws VerificationFailedException if verification fails.
    */
    @!APILevel[12, atomicservice : true]
    public static func ordered(
        statements: Array<VerifyStatement>
    ): Unit
    
    /**
    * Clears invocation log for the purposes of verification.
    * Expectations set during stub configugation will not be affected by this call.
    */
    @!APILevel[12, atomicservice : true]
    public static func clearInvocationLog(): Unit
    
    /**
    * Verifies no invocations were made on *mockObjects*.
    *
    * @param mocks objects that must not have any associated records in the invocation log.
    * @throws VerificationFailedException if verification fails.
    * @throws MockFrameworkException in the following cases:
    *     1. *mockObjects* is empty
    *     2. *mockObjects* contains references to objects that are not mocks or spies.
    *     3. *mockObjects* contains duplicate references.
    */
    @!APILevel[12, atomicservice : true]
    public static func noInteractions(mockObjects: Array<Object>): Unit
}

/**
* Collects statements for *unordered* verification.
*/
@!APILevel[12, atomicservice : true]
public class UnorderedVerifier {
    /**
    * Adds statement to this verification session.
    *
    * @param statement to be added.
    * @returns self.
    */
    @!APILevel[12, atomicservice : true]
    public func checkThat(statement: VerifyStatement): UnorderedVerifier
}

/**
* Collects statements for *ordered* verification.
*/
@!APILevel[12, atomicservice : true]
public class OrderedVerifier {
    /**
    * Adds statement to this verification session.
    *
    * @param statement to be added.
    * @returns self.
    */
    @!APILevel[12, atomicservice : true]
    public func checkThat(statement: VerifyStatement): OrderedVerifier
}

/**
* Represents a condition to be checked against an invocation log.
* It is defined by stub description - mock or spy object, some callable on that object and matchers for its arguments,
*    and cardinality - how many times the stub must have been invoked to satisfy the condition.
* Cardinality can only be set at most once for each statement.
* If cardinality was not set explicitly default cardinality will be assumed based on verification type.
* Cardinality cannot be reassigned after a statement was submitted to a verification session.
*/
@!APILevel[12, atomicservice : true]
public class VerifyStatement {
    /**
    * Constructs a *VerifyStatement*.
    * Do NOT call this directly. Use @Called macro to constuct *VerifyStatement*s.
    * API stability is not guaranteed for this function.
    */
    @!APILevel[12, atomicservice : true]
    public static func fromStub<R>(
        stubCall: () -> R,
        matchers: Array<ArgumentMatcher>,
        objName: Option<String>,
        declarationName: String,
        callDescription: String,
        lineNumber: Int64
    ): VerifyStatement
    
    /**
    * Specifies that this statement must match exactly once invocation.
    *
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func once(): VerifyStatement
    
    /**
    * Specifies that this statement must match one or more invocations.
    *
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func atLeastOnce(): VerifyStatement
    
    /**
    * Specifies that this statement must match *expectedTimes* or more invocation.
    *
    * @param expectedTimes exact number of invocations to be matched.
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func times(expectedTimes: Int64): VerifyStatement
    
    /**
    * Specifies that this statement must match at least *min* and at most *max* invocations.
    *
    * @param min minimum number of invocations to be matched.
    * @param max maximum number of invocations to be matched.
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func times(min!: Int64, max!: Int64): VerifyStatement
    
    /**
    * Specifies that this statement must match at least *minTimesExpected* invocations.
    *
    * @param minTimesExpected minimum number of invocations to be matched.
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func atLeastTimes(minTimesExpected: Int64): VerifyStatement
    
    /**
    * Specifies that this statement must not match any invocations.
    *
    * @returns self.
    * @throws MockFrameworkException if cardinality was already set and/or statement was already submitted to a verification session.
    */
    @!APILevel[12, atomicservice : true]
    public func never(): VerifyStatement
}

