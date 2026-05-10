// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unittest.mock.`internal`

import std.collection.ArrayList
import std.core.*
import std.collection.*

func createMock(handler: std.unittest.mock.`internal`.CallHandler): T

interface CallHandler {
    func onCall(call: std.unittest.mock.`internal`.Call): std.unittest.mock.`internal`.OnCall

    func requireMockObject(obj: T): T

    func recordStatic(callHandler: std.unittest.mock.`internal`.CallHandler): Unit

    func clearStatic: std.unittest.mock.`internal`.CallHandler

    func currentStatic: std.unittest.mock.`internal`.CallHandler
}

func createSpy(handler: std.unittest.mock.`internal`.CallHandler, objectToSpyOn: T): T

var staticCallHandlers: std.collection.ArrayList<std.unittest.mock.`internal`.CallHandler>

class UninitializedStaticCallHandler <: std.core.Exception {
    init() { /* compiled code */ }
}

struct Call {
    let typeArgs: std.core.Array<std.core.String>

    let receiver: std.core.Option<(std.core.Object, std.core.Array<std.core.String>)>

    let args: std.core.Array<std.core.Any>

    let funcInfo: std.unittest.mock.`internal`.FuncInfo

    init(_receiver: std.core.Option<std.core.Object>, _typeArgs: std.core.Array<std.core.ToString>, args: std.core.Array<std.core.Any>, funcInfo: std.unittest.mock.`internal`.FuncInfo) { /* compiled code */ }
}

enum OnCall {
    | ReturnZero

    | Return(std.core.Any)

    | Throw(std.core.Exception)

    | CallBase

    | ReturnDefault
}

func requireMockObject(obj: T): T

struct FuncInfo {
    let id: std.unittest.mock.`internal`.DeclId

    let params: std.core.Array<std.unittest.mock.`internal`.ParameterInfo>

    let typeParams: std.core.Array<std.core.String>

    let location: (std.core.String, Int64, Int64)

    let hasImplementation: Bool

    let outerDeclId: std.core.Option<std.unittest.mock.`internal`.DeclId>

    let hasDefaultValue: Bool

    let returnTypeName: std.core.String

    let isAccessible: Bool

    let kind: std.unittest.mock.`internal`.DeclKind

    init(id: std.unittest.mock.`internal`.DeclId, params: std.core.Array<std.unittest.mock.`internal`.ParameterInfo>, typeParams: std.core.Array<std.core.String>, location: (std.core.String, Int64, Int64), hasImplementation: Bool, outerDeclId: std.core.Option<std.unittest.mock.`internal`.DeclId>, hasDefaultValue: Bool, returnTypeName: std.core.String, isAccessible: Bool, kind: std.unittest.mock.`internal`.DeclKind) { /* compiled code */ }
}

struct DeclId <: std.core.Equatable<std.unittest.mock.`internal`.DeclId> & std.core.Hashable {
    operator func ==(that: std.unittest.mock.`internal`.DeclId): Bool

    operator func !=(that: std.unittest.mock.`internal`.DeclId): Bool

    func hashCode: Int64

    let mangledName: std.core.String

    let shortName: std.core.String

    init(mangledName: std.core.String, shortName: std.core.String) { /* compiled code */ }
}

struct ParameterInfo {
    let name: std.core.String

    let position: Int64

    let isNamed: Bool

    let hasDefaultValue: Bool

    init(name: std.core.String, position: Int64, isNamed: Bool, hasDefaultValue: Bool) { /* compiled code */ }
}

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

interface HasDefaultValueForStub {
    func defaultValueForStub: T
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Unit
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Int8
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Int16
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Int32
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Int64
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Float16
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Float32
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Float64
}

extend Extend <: HasDefaultValueForStub {
    func defaultValueForStub: Bool
}

extend String <: HasDefaultValueForStub {
    func defaultValueForStub: std.core.String
}

extend Option <: HasDefaultValueForStub {
    func defaultValueForStub: std.core.Option<T>
}

extend Array <: HasDefaultValueForStub {
    func defaultValueForStub: std.core.Array<T>
}

extend ArrayList <: HasDefaultValueForStub {
    func defaultValueForStub: std.collection.ArrayList<T>
}

extend HashSet <: HasDefaultValueForStub {
    func defaultValueForStub: std.collection.HashSet<T>
}

extend HashMap <: HasDefaultValueForStub {
    func defaultValueForStub: std.collection.HashMap<K, V>
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
    func getHandler: std.unittest.mock.`internal`.CallHandler
}

struct MockZeroValue {
    init() { /* compiled code */ }
}
