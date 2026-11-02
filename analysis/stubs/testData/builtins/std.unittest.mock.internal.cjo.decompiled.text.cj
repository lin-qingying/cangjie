// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.mock.internal

import std.collection.ArrayList
import std.core.*
import std.collection.*

@Frozen
@Intrinsic
public func createMock<T>(handler: std.unittest.mock.internal.CallHandler): T

public interface CallHandler {
    public open func onCall(call: std.unittest.mock.internal.Call): std.unittest.mock.internal.OnCall

    public open func requireMockObject<T>(obj: T): T

    public static func recordStatic(callHandler: std.unittest.mock.internal.CallHandler): Unit { /* compiled code */ }

    public static func clearStatic(): std.unittest.mock.internal.CallHandler { /* compiled code */ }

    public static func currentStatic(): std.unittest.mock.internal.CallHandler { /* compiled code */ }
}

@Frozen
@Intrinsic
public func createSpy<T>(handler: std.unittest.mock.internal.CallHandler, objectToSpyOn: T): T

var staticCallHandlers: std.collection.ArrayList<std.unittest.mock.internal.CallHandler>

public class UninitializedStaticCallHandler <: std.core.Exception {
    init() { /* compiled code */ }
}

public struct Call {
    public let typeArgs: std.core.Array<std.core.String>

    public let receiver: std.core.Option<(std.core.Object, std.core.Array<std.core.String>)>

    public let args: std.core.Array<std.core.Any>

    public let funcInfo: std.unittest.mock.internal.FuncInfo

    Call(_receiver: std.core.Option<std.core.Object>, _typeArgs: std.core.Array<std.core.ToString>, args: std.core.Array<std.core.Any>, funcInfo: std.unittest.mock.internal.FuncInfo) { /* compiled code */ }
}

public enum OnCall {
    | ReturnZero

    | Return(std.core.Any)

    | Throw(std.core.Exception)

    | CallBase

    | ReturnDefault
}

protected func requireMockObject<T>(obj: T): T { /* compiled code */ }

public struct FuncInfo {
    public let id: std.unittest.mock.internal.DeclId

    public let params: std.core.Array<std.unittest.mock.internal.ParameterInfo>

    public let typeParams: std.core.Array<std.core.String>

    public let location: (std.core.String, Int64, Int64)

    public let hasImplementation: Bool

    public let outerDeclId: std.core.Option<std.unittest.mock.internal.DeclId>

    public let hasDefaultValue: Bool

    public let returnTypeName: std.core.String

    public let isAccessible: Bool

    public let kind: std.unittest.mock.internal.DeclKind

    FuncInfo(id: std.unittest.mock.internal.DeclId, params: std.core.Array<std.unittest.mock.internal.ParameterInfo>, typeParams: std.core.Array<std.core.String>, location: (std.core.String, Int64, Int64), hasImplementation: Bool, outerDeclId: std.core.Option<std.unittest.mock.internal.DeclId>, hasDefaultValue: Bool, returnTypeName: std.core.String, isAccessible: Bool, kind: std.unittest.mock.internal.DeclKind) { /* compiled code */ }
}

public struct DeclId <: std.core.Equatable<std.unittest.mock.internal.DeclId> & std.core.Hashable {
    public operator func ==(that: std.unittest.mock.internal.DeclId): Bool { /* compiled code */ }

    public operator func !=(that: std.unittest.mock.internal.DeclId): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public let mangledName: std.core.String

    public let shortName: std.core.String

    DeclId(mangledName: std.core.String, shortName: std.core.String) { /* compiled code */ }
}

public struct ParameterInfo {
    public let name: std.core.String

    public let position: Int64

    public let isNamed: Bool

    public let hasDefaultValue: Bool

    ParameterInfo(name: std.core.String, position: Int64, isNamed: Bool, hasDefaultValue: Bool) { /* compiled code */ }
}

public enum DeclKind {
    | Method(std.core.String)

    | FieldGetter(std.core.String, Bool)

    | FieldSetter(std.core.String)

    | PropertyGetter(std.core.String, Bool)

    | PropertySetter(std.core.String)

    | TopLevelFunction(std.core.String)

    | StaticMethod(std.core.String)

    | StaticPropertyGetter(std.core.String)

    | StaticPropertySetter(std.core.String)

    | StaticFieldGetter(std.core.String)

    | StaticFieldSetter(std.core.String)

    | TopLevelVariableGetter(std.core.String)

    | TopLevelVariableSetter(std.core.String)
}

public interface HasDefaultValueForStub<T> {
    public static func defaultValueForStub(): T
}

extend Unit <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Unit { /* compiled code */ }
}

extend Int8 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Int8 { /* compiled code */ }
}

extend Int16 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Int16 { /* compiled code */ }
}

extend Int32 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Int32 { /* compiled code */ }
}

extend Int64 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Int64 { /* compiled code */ }
}

extend Float16 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Float16 { /* compiled code */ }
}

extend Float32 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Float32 { /* compiled code */ }
}

extend Float64 <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Float64 { /* compiled code */ }
}

extend Bool <: HasDefaultValueForStub {
    public static func defaultValueForStub(): Bool { /* compiled code */ }
}

extend std.core.String <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.core.String { /* compiled code */ }
}

extend std.core.Option<T> <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.core.Option<T> { /* compiled code */ }
}

extend std.core.Array<T> <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.core.Array<T> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.collection.ArrayList<T> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.collection.HashSet<T> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: HasDefaultValueForStub {
    public static func defaultValueForStub(): std.collection.HashMap<K, V> { /* compiled code */ }
}

public class IllegalMockCallException <: std.core.Exception {
    init() { /* compiled code */ }
}

public class NoDefaultValueForMockException <: std.core.Exception {
    init(typeName: std.core.String, defaultValueForStubInterfaceName: std.core.String) { /* compiled code */ }
}

public class MockReturnValueTypeMismatchException <: std.core.Exception {
    init(declarationReturnTypeName: std.core.String) { /* compiled code */ }
}

public interface Mocked {
    public open func getHandler(): std.unittest.mock.internal.CallHandler
}

public struct MockZeroValue {
    init() { /* compiled code */ }
}
