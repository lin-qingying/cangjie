// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.mock.internal

import std.collection.ArrayList
import std.core.*
import std.collection.*

@Frozen
func createMock<T>(handler: std.unittest.mock.internal.CallHandler): T { /* compiled code */ }

interface CallHandler {
    abstract open func onCall(call: std.unittest.mock.internal.Call): std.unittest.mock.internal.OnCall

    abstract open func requireMockObject<T>(obj: T): T

    static func recordStatic(callHandler: std.unittest.mock.internal.CallHandler): Unit { /* compiled code */ }

    static func clearStatic(): std.unittest.mock.internal.CallHandler { /* compiled code */ }

    static func currentStatic(): std.unittest.mock.internal.CallHandler { /* compiled code */ }
}

@Frozen
func createSpy<T>(handler: std.unittest.mock.internal.CallHandler, objectToSpyOn: T): T { /* compiled code */ }

var staticCallHandlers: std.collection.ArrayList<std.unittest.mock.internal.CallHandler>

class UninitializedStaticCallHandler <: std.core.Exception {
    init() { /* compiled code */ }
}

struct Call {
    let typeArgs: std.core.Array<std.core.String>

    let receiver: std.core.Option<(std.core.Object, std.core.Array<std.core.String>)>

    let args: std.core.Array<std.core.Any>

    let funcInfo: std.unittest.mock.internal.FuncInfo

    Call(_receiver: std.core.Option<std.core.Object>, _typeArgs: std.core.Array<std.core.ToString>, args: std.core.Array<std.core.Any>, funcInfo: std.unittest.mock.internal.FuncInfo) { /* compiled code */ }
}

@Annotation
enum OnCall {
    | ReturnZero

    | Return(std.core.Any)

    | Throw(std.core.Exception)

    | CallBase

    | ReturnDefault
}

func requireMockObject<T>(obj: T): T { /* compiled code */ }

struct FuncInfo {
    let id: std.unittest.mock.internal.DeclId

    let params: std.core.Array<std.unittest.mock.internal.ParameterInfo>

    let typeParams: std.core.Array<std.core.String>

    let location: (std.core.String, Int64, Int64)

    let hasImplementation: Bool

    let outerDeclId: std.core.Option<std.unittest.mock.internal.DeclId>

    let hasDefaultValue: Bool

    let returnTypeName: std.core.String

    let isAccessible: Bool

    let kind: std.unittest.mock.internal.DeclKind

    FuncInfo(id: std.unittest.mock.internal.DeclId, params: std.core.Array<std.unittest.mock.internal.ParameterInfo>, typeParams: std.core.Array<std.core.String>, location: (std.core.String, Int64, Int64), hasImplementation: Bool, outerDeclId: std.core.Option<std.unittest.mock.internal.DeclId>, hasDefaultValue: Bool, returnTypeName: std.core.String, isAccessible: Bool, kind: std.unittest.mock.internal.DeclKind) { /* compiled code */ }
}

struct DeclId <: std.core.Equatable<std.unittest.mock.internal.DeclId> & std.core.Hashable {
    operator func ==(that: std.unittest.mock.internal.DeclId): Bool { /* compiled code */ }

    operator func !=(that: std.unittest.mock.internal.DeclId): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    let mangledName: std.core.String

    let shortName: std.core.String

    DeclId(mangledName: std.core.String, shortName: std.core.String) { /* compiled code */ }
}

struct ParameterInfo {
    let name: std.core.String

    let position: Int64

    let isNamed: Bool

    let hasDefaultValue: Bool

    ParameterInfo(name: std.core.String, position: Int64, isNamed: Bool, hasDefaultValue: Bool) { /* compiled code */ }
}

@Annotation
enum DeclKind {
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

interface HasDefaultValueForStub<T> {
    abstract static func defaultValueForStub(): T
}

extend Unit <: HasDefaultValueForStub {
    static func defaultValueForStub(): Unit { /* compiled code */ }
}

extend Int8 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Int8 { /* compiled code */ }
}

extend Int16 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Int16 { /* compiled code */ }
}

extend Int32 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Int32 { /* compiled code */ }
}

extend Int64 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Int64 { /* compiled code */ }
}

extend Float16 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Float16 { /* compiled code */ }
}

extend Float32 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Float32 { /* compiled code */ }
}

extend Float64 <: HasDefaultValueForStub {
    static func defaultValueForStub(): Float64 { /* compiled code */ }
}

extend Bool <: HasDefaultValueForStub {
    static func defaultValueForStub(): Bool { /* compiled code */ }
}

extend std.core.String <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.core.String { /* compiled code */ }
}

extend std.core.Option<T> <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.core.Option<T> { /* compiled code */ }
}

extend std.core.Array<T> <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.core.Array<T> { /* compiled code */ }
}

extend std.collection.ArrayList<T> <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.collection.ArrayList<T> { /* compiled code */ }
}

extend std.collection.HashSet<T> <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.collection.HashSet<T> { /* compiled code */ }
}

extend std.collection.HashMap<K, V> <: HasDefaultValueForStub {
    static func defaultValueForStub(): std.collection.HashMap<K, V> { /* compiled code */ }
}

class IllegalMockCallException <: std.core.Exception {
    init() { /* compiled code */ }
}

class NoDefaultValueForMockException <: std.core.Exception {
    init(typeName: std.core.String, defaultValueForStubInterfaceName: std.core.String) { /* compiled code */ }
}

class MockReturnValueTypeMismatchException <: std.core.Exception {
    init(declarationReturnTypeName: std.core.String) { /* compiled code */ }
}

interface Mocked {
    abstract open func getHandler(): std.unittest.mock.internal.CallHandler
}

struct MockZeroValue {
    init() { /* compiled code */ }
}
