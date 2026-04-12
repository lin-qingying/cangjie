// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.




package std.unittest.mock.internal
import std.collection.*


@!APILevel[12, atomicservice : true]
public interface CallHandler {
}

@!APILevel[12, atomicservice : true]
public enum OnCall {
    @!APILevel[12, atomicservice : true]
    ReturnZero |
    @!APILevel[12, atomicservice : true]
    Return(Any) |
    @!APILevel[12, atomicservice : true]
    Throw(Exception) |
    @!APILevel[12, atomicservice : true]
    CallBase |
    @!APILevel[12, atomicservice : true]
    ReturnDefault
}

@!APILevel[12, atomicservice : true]
public struct Call {
    @!APILevel[12, atomicservice : true]
    public let typeArgs: Array<String>
    
    @!APILevel[12, atomicservice : true]
    public let receiver: Option<(Object, Array<String>)>
    
    @!APILevel[12, atomicservice : true]
    public Call(
        _receiver: Option<Object>,
        _typeArgs: Array<ToString>, // merged type arguments for a call and for the outer declaration
        public let args: Array<Any>,
        public let funcInfo: FuncInfo
    )
}

@!APILevel[12, atomicservice : true]
public struct FuncInfo {
    @!APILevel[12, atomicservice : true]
    public FuncInfo(
        public let id: DeclId,
        public let params: Array<ParameterInfo>,
        public let typeParams: Array<String>,
        public let location: (String, Int64, Int64),
        public let hasImplementation: Bool,
        public let outerDeclId: Option<DeclId>,
        public let hasDefaultValue: Bool,
        public let returnTypeName: String,
        public let isAccessible: Bool,
        public let kind: DeclKind
    )
}

@!APILevel[12, atomicservice : true]
public enum DeclKind {
    @!APILevel[12, atomicservice : true]
    Method(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    FieldGetter(/*fieldName: */String, /*hasSetter: */Bool) |
    @!APILevel[12, atomicservice : true]
    FieldSetter(/*fieldName:*/ String) |
    @!APILevel[12, atomicservice : true]
    PropertyGetter(/*propertyName:*/String, /*hasSetter:*/ Bool) |
    @!APILevel[12, atomicservice : true]
    PropertySetter(/*propertyName:*/ String) |
    @!APILevel[12, atomicservice : true]
    TopLevelFunction(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    StaticMethod(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    StaticPropertyGetter(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    StaticPropertySetter(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    StaticFieldGetter(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    StaticFieldSetter(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    TopLevelVariableGetter(/*name:*/ String) |
    @!APILevel[12, atomicservice : true]
    TopLevelVariableSetter(/*name:*/ String)
}

@!APILevel[12, atomicservice : true]
public struct ParameterInfo {
    @!APILevel[12, atomicservice : true]
    public ParameterInfo(
        public let name: String,
        public let position: Int64,
        public let isNamed: Bool,
        public let hasDefaultValue: Bool
    )
}

@!APILevel[12, atomicservice : true]
public struct DeclId <: Equatable<DeclId> & Hashable {
    @!APILevel[12, atomicservice : true]
    public DeclId(
        public let mangledName: String,
        public let shortName: String
    )
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: DeclId): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: DeclId): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
}

@!APILevel[12, atomicservice : true]
public interface HasDefaultValueForStub<T> {
}

extend Unit <: HasDefaultValueForStub<Unit> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Unit
}

extend Int8 <: HasDefaultValueForStub<Int8> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Int8
}

extend Int16 <: HasDefaultValueForStub<Int16> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Int16
}

extend Int32 <: HasDefaultValueForStub<Int32> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Int32
}

extend Int64 <: HasDefaultValueForStub<Int64> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Int64
}

extend Float16 <: HasDefaultValueForStub<Float16> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Float16
}

extend Float32 <: HasDefaultValueForStub<Float32> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Float32
}

extend Float64 <: HasDefaultValueForStub<Float64> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Float64
}

extend Bool <: HasDefaultValueForStub<Bool> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Bool
}

extend String <: HasDefaultValueForStub<String> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): String
}

extend<T> Option<T> <: HasDefaultValueForStub<Option<T>> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Option<T>
}

extend<T> Array<T> <: HasDefaultValueForStub<Array<T>> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): Array<T>
}

extend<T> ArrayList<T> <: HasDefaultValueForStub<ArrayList<T>> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): ArrayList<T>
}

extend<T> HashSet<T> <: HasDefaultValueForStub<HashSet<T>> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): HashSet<T>
}

extend<K, V> HashMap<K, V> <: HasDefaultValueForStub<HashMap<K, V>> {
    @!APILevel[12, atomicservice : true]
    public static func defaultValueForStub(): HashMap<K, V>
}

/**
* Used mock calls in the runtime-error mocking mode (compiled with --mock=runtime-error),
* or inside a generic function which isn't marked with @Frozen annotation
*/
@!APILevel[12, atomicservice : true]
public class IllegalMockCallException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
}

/**
* No default value provided for a type in the `ReturnsDefaults` mode
*/
@!APILevel[12, atomicservice : true]
public class NoDefaultValueForMockException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init(typeName: String, defaultValueForStubInterfaceName: String)
}

/**
* Failed to cast the provided stubbed return value to the real declaration's return type
*/
@!APILevel[12, atomicservice : true]
public class MockReturnValueTypeMismatchException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init(declarationReturnTypeName: String)
}

@!APILevel[12, atomicservice : true]
public class UninitializedStaticCallHandler <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
}

@!APILevel[12, atomicservice : true]
public class WrongMockObjectTypeException <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
}

@Frozen@Intrinsic
@!APILevel[12, atomicservice : true]
public unsafe func createMock<T>(handler: CallHandler): T

@Frozen@Intrinsic
@!APILevel[12, atomicservice : true]
public unsafe func createSpy<T>(handler: CallHandler, objectToSpyOn: T): T

@!APILevel[12, atomicservice : true]
public interface Mocked {
}

// This value is used in calls to `zeroValue` to correctly// detect zero values in mocked functions.
@!APILevel[12, atomicservice : true]
public struct MockZeroValue {
}

