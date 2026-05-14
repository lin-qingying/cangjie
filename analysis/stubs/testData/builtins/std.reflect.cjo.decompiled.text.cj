// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.reflect

import std.collection.*
import std.sync.*
import std.core.*
import std.fs.*

let constructorName: std.core.String

let FF_BYTE: UInt8

let NOTHING_QN: std.core.String

let UNIT_QN: std.core.String

let CHAR_QN: std.core.String

let BOOL_QN: std.core.String

let INT8_QN: std.core.String

let INT16_QN: std.core.String

let INT32_QN: std.core.String

let INT64_QN: std.core.String

let INT_NATIVE_QN: std.core.String

let U_INT8_QN: std.core.String

let U_INT16_QN: std.core.String

let U_INT32_QN: std.core.String

let U_INT64_QN: std.core.String

let U_INT_NATIVE_QN: std.core.String

let FLOAT16_QN: std.core.String

let FLOAT32_QN: std.core.String

let FLOAT64_QN: std.core.String

let PACKAGE_LOAD_FAIL: UIntNative

let MULTI_PACKAGE_INFO: UIntNative

let REPEATED_PACKAGE_LOAD: UIntNative

let REPEATED_FILENAME_LOAD: UIntNative

let MODULE_SUFFIX: std.core.String

let UUID_OFFSET: Int64

class ClassTypeInfo <: std.reflect.TypeInfo {
    var _constructors: std.core.Option<std.core.Collection<std.reflect.ConstructorInfo>>

    var _instanceVariables: std.core.Option<std.core.Collection<std.reflect.InstanceVariableInfo>>

    var _staticVariables: std.core.Option<std.core.Collection<std.reflect.StaticVariableInfo>>

    var _superClass: std.core.Option<std.core.Option<std.reflect.ClassTypeInfo>>

    var _sealedSubclasses: std.core.Option<std.core.Collection<std.reflect.ClassTypeInfo>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    redef static func `get`(qualifiedName: std.core.String): std.reflect.ClassTypeInfo { /* compiled code */ }

    redef static func of(a: std.core.Any): std.reflect.ClassTypeInfo { /* compiled code */ }

    static func of(a: std.core.Object): std.reflect.ClassTypeInfo { /* compiled code */ }

    redef static func of(): std.reflect.ClassTypeInfo { /* compiled code */ }

    prop constructors: std.core.Collection<std.reflect.ConstructorInfo> {
        get() { /* compiled code */ }
    }

    prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo> {
        get() { /* compiled code */ }
    }

    prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo> {
        get() { /* compiled code */ }
    }

    prop superClass: std.core.Option<std.reflect.ClassTypeInfo> {
        get() { /* compiled code */ }
    }

    prop sealedSubclasses: std.core.Collection<std.reflect.ClassTypeInfo> {
        get() { /* compiled code */ }
    }

    func construct(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func isOpen(): Bool { /* compiled code */ }

    func isAbstract(): Bool { /* compiled code */ }

    func isSealed(): Bool { /* compiled code */ }

    func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo { /* compiled code */ }

    func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo { /* compiled code */ }

    func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo { /* compiled code */ }
}

class ConstructorInfo <: std.core.Object & std.core.Equatable<std.reflect.ConstructorInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _isAbstract: std.core.Option<Bool>

    var _declaringTypeInfo: CPointer<Unit>

    init(infoPointer: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func apply(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.ConstructorInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.ConstructorInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class InstanceVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.InstanceVariableInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    let _isStruct: Bool

    var _mutable: std.core.Option<Bool>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _declaringTypeInfo: CPointer<Unit>

    init(cp: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>, isStruct: Bool) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isMutable(): Bool { /* compiled code */ }

    func getValue(instance: std.core.Any): std.core.Any { /* compiled code */ }

    func setValue(instance: std.core.Any, newValue: std.core.Any): Unit { /* compiled code */ }

    func checkDeclaringClassType(instance: std.core.Any): Unit { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.InstanceVariableInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.InstanceVariableInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class StaticVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticVariableInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    let _isStruct: Bool

    var _mutable: std.core.Option<Bool>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _declaringTypeInfo: CPointer<Unit>

    init(cp: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isMutable(): Bool { /* compiled code */ }

    func getValue(): std.core.Any { /* compiled code */ }

    func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.StaticVariableInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.StaticVariableInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class TypeInfo <: std.core.Object & std.core.Equatable<std.reflect.TypeInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _qualifiedName: std.core.Option<std.core.String>

    var _name: std.core.Option<std.core.String>

    var _instanceFunctions: std.core.Option<std.core.Collection<std.reflect.InstanceFunctionInfo>>

    var _staticFunctions: std.core.Option<std.core.Collection<std.reflect.StaticFunctionInfo>>

    var _instanceProperties: std.core.Option<std.core.Collection<std.reflect.InstancePropertyInfo>>

    var _staticProperties: std.core.Option<std.core.Collection<std.reflect.StaticPropertyInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _superInterfaces: std.core.Option<std.core.Collection<std.reflect.InterfaceTypeInfo>>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var typeTemplate: std.reflect.TypeTemplate

    var genericParams: std.core.Array<std.reflect.TypeInfo>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    static func of(): std.reflect.TypeInfo { /* compiled code */ }

    static func of(a: std.core.Any): std.reflect.TypeInfo { /* compiled code */ }

    static func of(a: std.core.Object): std.reflect.ClassTypeInfo { /* compiled code */ }

    static func `get`(qualifiedName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    static func getOrCreate(typeInfoPointer: CPointer<Unit>): std.reflect.TypeInfo { /* compiled code */ }

    static func createGenericTypeInfo(typeInfoPointer: CPointer<Unit>, qualifiedName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    static func createConcreteTypeInfo(typeInfoPointer: CPointer<Unit>, qualifiedName: std.core.String, uuid: std.core.Option<std.core.String>): std.reflect.TypeInfo { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop qualifiedName: std.core.String {
        get() { /* compiled code */ }
    }

    prop instanceFunctions: std.core.Collection<std.reflect.InstanceFunctionInfo> {
        get() { /* compiled code */ }
    }

    prop staticFunctions: std.core.Collection<std.reflect.StaticFunctionInfo> {
        get() { /* compiled code */ }
    }

    prop instanceProperties: std.core.Collection<std.reflect.InstancePropertyInfo> {
        get() { /* compiled code */ }
    }

    prop staticProperties: std.core.Collection<std.reflect.StaticPropertyInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    prop superInterfaces: std.core.Collection<std.reflect.InterfaceTypeInfo> {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    func isSubtypeOf(supertype: std.reflect.TypeInfo): Bool { /* compiled code */ }

    func getInstanceFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.InstanceFunctionInfo { /* compiled code */ }

    func getInstanceFunctions(name: std.core.String): std.core.Array<std.reflect.InstanceFunctionInfo> { /* compiled code */ }

    func getStaticFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.StaticFunctionInfo { /* compiled code */ }

    func getStaticFunctions(name: std.core.String): std.core.Array<std.reflect.StaticFunctionInfo> { /* compiled code */ }

    func getInstanceProperty(name: std.core.String): std.reflect.InstancePropertyInfo { /* compiled code */ }

    func getStaticProperty(name: std.core.String): std.reflect.StaticPropertyInfo { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.TypeInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.TypeInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func getInstanceFunctions(): Unit { /* compiled code */ }

    func getStaticFunctions(): Unit { /* compiled code */ }
}

class ReflectException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class InfoNotFoundException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class MisMatchException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class IllegalSetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class IllegalTypeException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class InvocationTargetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

class GenericTypeInfo <: std.reflect.TypeInfo & std.core.Equatable<std.reflect.GenericTypeInfo> {
    operator func ==(that: std.reflect.GenericTypeInfo): Bool { /* compiled code */ }

    init(cp: CPointer<Unit>) { /* compiled code */ }
}

class InstanceFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.InstanceFunctionInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _name: std.core.Option<std.core.String>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _returnType: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    var _declaringTypeInfo: CPointer<Unit>

    init(cp: CPointer<Unit>, info: CPointer<Unit>) { /* compiled code */ }

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isOpen(): Bool { /* compiled code */ }

    func isAbstract(): Bool { /* compiled code */ }

    func apply(instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func apply(instance: std.core.Any, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.InstanceFunctionInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.InstanceFunctionInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class ParameterInfo <: std.core.Object & std.core.Equatable<std.reflect.ParameterInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _index: std.core.Option<Int64>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop index: Int64 {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.ParameterInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.ParameterInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

enum ModifierInfo <: std.core.Equatable<std.reflect.ModifierInfo> & std.core.Hashable & std.core.ToString {
    | Open

    | Override

    | Redef

    | Abstract

    | Sealed

    | Mut

    | Static

    static func byMask(mask: UInt32): std.core.Collection<std.reflect.ModifierInfo> { /* compiled code */ }

    static func isAbstract(mask: UInt32): Bool { /* compiled code */ }

    static func isPublic(mask: UInt32): Bool { /* compiled code */ }

    static func isMutable(mask: UInt32): Bool { /* compiled code */ }

    override operator func ==(that: std.reflect.ModifierInfo): Bool { /* compiled code */ }

    override operator func !=(that: std.reflect.ModifierInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    override func toString(): std.core.String { /* compiled code */ }
}

class StaticFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticFunctionInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _name: std.core.Option<std.core.String>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    var _returnType: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _declaringTypeInfo: CPointer<Unit>

    init(cp: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func apply(thisType: std.reflect.TypeInfo, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func apply(thisType: std.reflect.TypeInfo, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.StaticFunctionInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.StaticFunctionInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class InstancePropertyInfo <: std.core.Object & std.core.Equatable<std.reflect.InstancePropertyInfo> & std.core.Hashable & std.core.ToString {
    let _name: std.core.String

    var _getterInfo: std.core.Option<std.reflect.InstanceFunctionInfo>

    var _setterInfo: std.core.Option<std.reflect.InstanceFunctionInfo>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _declaringTypeInfo: CPointer<Unit>

    init(name: std.core.String, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isOpen(): Bool { /* compiled code */ }

    func isAbstract(): Bool { /* compiled code */ }

    func isMutable(): Bool { /* compiled code */ }

    func getValue(instance: std.core.Any): std.core.Any { /* compiled code */ }

    func setValue(instance: std.core.Any, newValue: std.core.Any): Unit { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.InstancePropertyInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.InstancePropertyInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func setFunc(isGetter: Bool, pointer: CPointer<Unit>): Unit { /* compiled code */ }
}

class StaticPropertyInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticPropertyInfo> & std.core.Hashable & std.core.ToString {
    let _name: std.core.String

    var _getterInfo: std.core.Option<std.reflect.StaticFunctionInfo>

    var _setterInfo: std.core.Option<std.reflect.StaticFunctionInfo>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _declaringTypeInfo: CPointer<Unit>

    init(name: std.core.String, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isMutable(): Bool { /* compiled code */ }

    func getValue(): std.core.Any { /* compiled code */ }

    func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.StaticPropertyInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.StaticPropertyInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }

    func setFunc(isGetter: Bool, pointer: CPointer<Unit>): Unit { /* compiled code */ }
}

class GlobalFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalFunctionInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _name: std.core.Option<std.core.String>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    var _returnType: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func apply(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func apply(genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.GlobalFunctionInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.GlobalFunctionInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

func getFuncName(parentPointer: CPointer<Unit>): std.core.String { /* compiled code */ }

func getParameters(cp: CPointer<Unit>): std.collection.ArrayList<std.reflect.ParameterInfo> { /* compiled code */ }

func checkMethodDeclaringClassType(instance: std.core.Any, declaringTypeInfo: CPointer<Unit>): Unit { /* compiled code */ }

func checkParamaterSize(genericParams: std.core.Collection<std.reflect.GenericTypeInfo>, actualParamSize: Int64, genericTypeParameters: std.core.Collection<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): Unit { /* compiled code */ }

class InterfaceTypeInfo <: std.reflect.TypeInfo {
    var _sealedSubtypes: std.core.Option<std.core.Collection<std.reflect.TypeInfo>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    redef static func `get`(qualifiedName: std.core.String): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    redef static func of(a: std.core.Any): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    redef static func of(): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    prop sealedSubtypes: std.core.Collection<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    func isSealed(): Bool { /* compiled code */ }
}

func findSubtypeFromPackage(curType: std.reflect.TypeInfo): std.core.Array<T> { /* compiled code */ }

func isInterface(ti: CPointer<Unit>): Bool { /* compiled code */ }

func isStruct(ti: CPointer<Unit>): Bool { /* compiled code */ }

func isPrimitive(ti: CPointer<Unit>): Bool { /* compiled code */ }

func isClass(ti: CPointer<Unit>): Bool { /* compiled code */ }

func isGeneric(ti: CPointer<Unit>): Bool { /* compiled code */ }

func getOrCreateTypeInfoForReflect(typeTemplate: CPointer<Unit>, typeArgs: std.core.Array<CPointer<Unit>>): CPointer<Unit> { /* compiled code */ }

func getTypeTemplate(name: CPointer<UInt8>): CPointer<Unit> { /* compiled code */ }

func checkMethodActualArgs(methodInfo: CPointer<Unit>, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>): Bool { /* compiled code */ }

func methodEntryPointIsNull(methodInfo: CPointer<Unit>): Bool { /* compiled code */ }

func isReflectUnsupportedType(ti: CPointer<Unit>): Bool { /* compiled code */ }

func getTypeForAny(obj: std.core.Any): CPointer<Unit> { /* compiled code */ }

func getTypeByMangledName(): CPointer<Unit> { /* compiled code */ }

func getTypeName(obj: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getTypeByQualifiedName(qualifiedName: CPointer<UInt8>): CPointer<Unit> { /* compiled code */ }

func getTypeQualifiedNameLength(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getTypeQualifiedName(ti: CPointer<Unit>, name: CPointer<UInt8>): Unit { /* compiled code */ }

func getNumOfInterface(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getInterface(ti: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func isSubType(ti: CPointer<Unit>, superTypeclassInfo: CPointer<Unit>): Bool { /* compiled code */ }

func getTypeInfoModifier(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getTypeInfoAnnotations(ti: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func getObjClass(obj: std.core.Object): CPointer<Unit> { /* compiled code */ }

func getSuperTypeInfo(ti: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getNumOfInstanceMethodInfos(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getInstanceMethodInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getNumOfStaticMethodInfos(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getStaticMethodInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getMethodName(methodInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getParameterInfo(methodInfo: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getMethodReturnType(methodInfo: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getMethodModifier(methodInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getMethodAnnotations(methodInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func getNumOfActualParameters(methodInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getNumOfGenericParameters(methodInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getActualParameterInfo(methodInfo: CPointer<Unit>, idx: UInt32): CPointer<Unit> { /* compiled code */ }

func getGenericParameterInfo(methodInfo: CPointer<Unit>, idx: UInt32): CPointer<Unit> { /* compiled code */ }

func getNumOfInstanceFieldInfos(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getInstanceFieldInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getNumOfStaticFieldInfos(ti: CPointer<Unit>): UInt32 { /* compiled code */ }

func getStaticFieldInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getStaticFieldName(fieldInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getStaticFieldType(fieldInfo: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getStaticFieldAnnotations(fieldInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func getInstanceFieldName(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getInstanceFieldType(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getInstanceFieldModifier(fieldInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getStaticFieldModifier(fieldInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getInstanceFieldAnnotations(fieldInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func getInstanceFieldValue(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>, instance: std.core.Any): std.core.Any { /* compiled code */ }

func setInstanceFieldValue(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>, instance: std.core.Any, value: std.core.Any): Unit { /* compiled code */ }

func getStaticFieldValue(fieldInfo: CPointer<Unit>): std.core.Any { /* compiled code */ }

func setStaticFieldValue(fieldInfo: CPointer<Unit>, value: std.core.Any): Unit { /* compiled code */ }

func getParameterIndex(parameterInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getParameterName(parameterInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getParameterType(parameterInfo: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getParameterAnnotations(parameterInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func applyCJInstanceMethod(methodInfo: CPointer<Unit>, instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

func applyCJStaticMethod(methodInfo: CPointer<Unit>, args: std.core.Array<std.core.Any>, thisType: CPointer<Unit>): std.core.Any { /* compiled code */ }

func applyCJGenericInstanceMethod(methodInfo: CPointer<Unit>, instance: std.core.Any, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

func applyCJGenericStaticMethod(methodInfo: CPointer<Unit>, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>, thisType: CPointer<Unit>): std.core.Any { /* compiled code */ }

func getRelatedPackageInfo(packageInfo: CPointer<Unit>): CPointer<Unit> { /* compiled code */ }

func getPackageName(packageInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getPackageNumOfTypeInfos(packageInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getPackageTypeInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getPackageNumOfGlobalMethodInfos(packageInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getPackageGlobalMethodInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func getPackageNumOfGlobalFieldInfos(packageInfo: CPointer<Unit>): UInt32 { /* compiled code */ }

func getPackageGlobalFieldInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit> { /* compiled code */ }

func loadPackage(path: CPointer<UInt8>): CPointer<Unit> { /* compiled code */ }

func getPackageByQualifiedName(qualifiedName: CPointer<UInt8>): CPointer<Unit> { /* compiled code */ }

func getPackageVersion(packageInfo: CPointer<Unit>): CPointer<UInt8> { /* compiled code */ }

func getSubPackages(packageInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any { /* compiled code */ }

func containsModifier(source: std.core.Collection<std.reflect.ModifierInfo>, modifier: std.reflect.ModifierInfo): Bool { /* compiled code */ }

func getMask(modifier: std.reflect.ModifierInfo): UInt32 { /* compiled code */ }

class PackageInfo <: std.core.Object & std.core.Equatable<std.reflect.PackageInfo> & std.core.Hashable & std.core.ToString {
    let _packageInfo: CPointer<Unit>

    var _qualifiedName: std.core.Option<std.core.String>

    var _name: std.core.Option<std.core.String>

    var _typeInfos: std.core.Option<std.core.Collection<std.reflect.TypeInfo>>

    var _variables: std.core.Option<std.core.Collection<std.reflect.GlobalVariableInfo>>

    var _functions: std.core.Option<std.core.Collection<std.reflect.GlobalFunctionInfo>>

    var _version: std.core.Option<std.core.String>

    var _rootPackage: std.core.Option<std.reflect.PackageInfo>

    var _noPublicInfoList: std.collection.ArrayList<std.reflect.TypeInfo>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    static func `get`(qualifiedName: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    static func load(path: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    prop qualifiedName: std.core.String {
        get() { /* compiled code */ }
    }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop noPublicInfos: std.collection.ArrayList<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    prop typeInfos: std.core.Collection<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    prop version: std.core.String {
        get() { /* compiled code */ }
    }

    prop subPackages: std.core.Collection<std.reflect.PackageInfo> {
        get() { /* compiled code */ }
    }

    prop parentPackage: std.reflect.PackageInfo {
        get() { /* compiled code */ }
    }

    prop rootPackage: std.reflect.PackageInfo {
        get() { /* compiled code */ }
    }

    func getSubPackage(qualifiedName: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    func getTypeInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.TypeInfo>): Unit { /* compiled code */ }

    prop variables: std.core.Collection<std.reflect.GlobalVariableInfo> {
        get() { /* compiled code */ }
    }

    func getGlobalVariableInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalVariableInfo>): Unit { /* compiled code */ }

    prop functions: std.core.Collection<std.reflect.GlobalFunctionInfo> {
        get() { /* compiled code */ }
    }

    func getGlobalFunctionInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalFunctionInfo>): Unit { /* compiled code */ }

    func getTypeInfo(qualifiedTypeName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    func getVariable(name: std.core.String): std.reflect.GlobalVariableInfo { /* compiled code */ }

    func getFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.GlobalFunctionInfo { /* compiled code */ }

    func getFunctions(name: std.core.String): std.core.Array<std.reflect.GlobalFunctionInfo> { /* compiled code */ }

    operator func ==(that: std.reflect.PackageInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.PackageInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class GlobalVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalVariableInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _mutable: std.core.Option<Bool>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop name: std.core.String {
        get() { /* compiled code */ }
    }

    prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    func isMutable(): Bool { /* compiled code */ }

    func getValue(): std.core.Any { /* compiled code */ }

    func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    func findAnnotation(): std.core.Option<T> { /* compiled code */ }

    operator func ==(that: std.reflect.GlobalVariableInfo): Bool { /* compiled code */ }

    operator func !=(that: std.reflect.GlobalVariableInfo): Bool { /* compiled code */ }

    func hashCode(): Int64 { /* compiled code */ }

    func toString(): std.core.String { /* compiled code */ }
}

class PrimitiveTypeInfo <: std.reflect.TypeInfo {
    let PRIMITIVES: std.core.Array<std.core.String>

    let ARRAY_OBJECT: std.core.Array<std.core.Object>

    init(cp: CPointer<Unit>, name: std.core.String) { /* compiled code */ }

    redef static func `get`(qualifiedName: std.core.String): std.reflect.PrimitiveTypeInfo { /* compiled code */ }

    redef static func of(a: std.core.Any): std.reflect.PrimitiveTypeInfo { /* compiled code */ }

    redef static func of(): std.reflect.PrimitiveTypeInfo { /* compiled code */ }
}

func checkPrimitive(name: std.core.String): (Bool, std.core.String, std.core.String) { /* compiled code */ }

class StructTypeInfo <: std.reflect.TypeInfo {
    var _constructors: std.core.Option<std.core.Collection<std.reflect.ConstructorInfo>>

    var _instanceVariables: std.core.Option<std.core.Collection<std.reflect.InstanceVariableInfo>>

    var _staticVariables: std.core.Option<std.core.Collection<std.reflect.StaticVariableInfo>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    redef static func `get`(qualifiedName: std.core.String): std.reflect.StructTypeInfo { /* compiled code */ }

    redef static func of(a: std.core.Any): std.reflect.StructTypeInfo { /* compiled code */ }

    redef static func of(): std.reflect.StructTypeInfo { /* compiled code */ }

    prop constructors: std.core.Collection<std.reflect.ConstructorInfo> {
        get() { /* compiled code */ }
    }

    prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo> {
        get() { /* compiled code */ }
    }

    prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo> {
        get() { /* compiled code */ }
    }

    func construct(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo { /* compiled code */ }

    func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo { /* compiled code */ }

    func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo { /* compiled code */ }
}

let TYPE_INFO_CREATE_LOCK: std.sync.Mutex

class TypeTemplate {
    let _info: CPointer<Unit>

    init(info: CPointer<Unit>) { /* compiled code */ }

    static func `get`(typeinfoName: std.core.String): std.reflect.TypeTemplate { /* compiled code */ }

    prop info: CPointer<Unit> {
        get() { /* compiled code */ }
    }
}

class TypeTemplateParser {
    var templates: std.collection.ArrayList<std.core.String>

    var data: std.collection.ArrayList<std.collection.ArrayList<std.core.String>>

    var map: std.collection.HashMap<std.core.String, CPointer<Unit>>

    func parse(string: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    func parseGenericString(name: std.core.Array<UInt8>, begin: Int64, end: Int64): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

type Annotation = std.core.Object

func parseParameterTypes(parameters: std.core.String): std.core.Array<std.reflect.TypeInfo> { /* compiled code */ }

func getByNameAndParamTypes(source: std.core.Iterable<T>, name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>, message: std.core.String, nameAndParamsGetter: (T) -> (std.core.String, std.collection.ReadOnlyList<std.reflect.ParameterInfo>)): T { /* compiled code */ }

func getByName(source: std.core.Iterable<T>, name: std.core.String, message: std.core.String, nameGetter: (T) -> std.core.String): T { /* compiled code */ }

func getTypeNameByQualifiedName(qualifiedName: std.core.String): std.core.String { /* compiled code */ }

func checkArgsSizeAndType(parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>, args: std.core.Array<std.core.Any>): Unit { /* compiled code */ }

func checkValueType(newValue: std.core.Any, typeInfo: std.reflect.TypeInfo): Unit { /* compiled code */ }

func findAnnotation(annotations: std.core.Collection<std.core.Object>): std.core.Option<T> { /* compiled code */ }

func getFieldDeclaringClassType(declaringTypeInfo: CPointer<Unit>): std.reflect.TypeInfo { /* compiled code */ }
