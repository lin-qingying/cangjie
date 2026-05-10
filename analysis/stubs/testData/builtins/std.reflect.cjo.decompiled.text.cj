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

    func `get`(qualifiedName: std.core.String): std.reflect.ClassTypeInfo

    func of(a: std.core.Any): std.reflect.ClassTypeInfo

    func of(a: std.core.Object): std.reflect.ClassTypeInfo

    func of: std.reflect.ClassTypeInfo

    prop constructors: std.core.Collection<std.reflect.ConstructorInfo>

    prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo>

    prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo>

    prop superClass: std.core.Option<std.reflect.ClassTypeInfo>

    prop sealedSubclasses: std.core.Collection<std.reflect.ClassTypeInfo>

    func construct(args: std.core.Array<std.core.Any>): std.core.Any

    func isOpen: Bool

    func isAbstract: Bool

    func isSealed: Bool

    func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo

    func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo

    func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo
}

class ConstructorInfo <: std.core.Object & std.core.Equatable<std.reflect.ConstructorInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    var _isAbstract: std.core.Option<Bool>

    var _declaringTypeInfo: CPointer<Unit>

    init(infoPointer: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>) { /* compiled code */ }

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func apply(args: std.core.Array<std.core.Any>): std.core.Any

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.ConstructorInfo): Bool

    operator func !=(that: std.reflect.ConstructorInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
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

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func isMutable: Bool

    func getValue(instance: std.core.Any): std.core.Any

    func setValue(instance: std.core.Any, newValue: std.core.Any): Unit

    func checkDeclaringClassType(instance: std.core.Any): Unit

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.InstanceVariableInfo): Bool

    operator func !=(that: std.reflect.InstanceVariableInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
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

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func isMutable: Bool

    func getValue: std.core.Any

    func setValue(newValue: std.core.Any): Unit

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.StaticVariableInfo): Bool

    operator func !=(that: std.reflect.StaticVariableInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
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

    func of: std.reflect.TypeInfo

    func of(a: std.core.Any): std.reflect.TypeInfo

    func of(a: std.core.Object): std.reflect.ClassTypeInfo

    func `get`(qualifiedName: std.core.String): std.reflect.TypeInfo

    func getOrCreate(typeInfoPointer: CPointer<Unit>): std.reflect.TypeInfo

    func createGenericTypeInfo(typeInfoPointer: CPointer<Unit>, qualifiedName: std.core.String): std.reflect.TypeInfo

    func createConcreteTypeInfo(typeInfoPointer: CPointer<Unit>, qualifiedName: std.core.String, uuid: std.core.Option<std.core.String>): std.reflect.TypeInfo

    prop name: std.core.String

    prop qualifiedName: std.core.String

    prop instanceFunctions: std.core.Collection<std.reflect.InstanceFunctionInfo>

    prop staticFunctions: std.core.Collection<std.reflect.StaticFunctionInfo>

    prop instanceProperties: std.core.Collection<std.reflect.InstancePropertyInfo>

    prop staticProperties: std.core.Collection<std.reflect.StaticPropertyInfo>

    prop annotations: std.core.Collection<std.core.Object>

    prop superInterfaces: std.core.Collection<std.reflect.InterfaceTypeInfo>

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    func isSubtypeOf(supertype: std.reflect.TypeInfo): Bool

    func getInstanceFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.InstanceFunctionInfo

    func getInstanceFunctions(name: std.core.String): std.core.Array<std.reflect.InstanceFunctionInfo>

    func getStaticFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.StaticFunctionInfo

    func getStaticFunctions(name: std.core.String): std.core.Array<std.reflect.StaticFunctionInfo>

    func getInstanceProperty(name: std.core.String): std.reflect.InstancePropertyInfo

    func getStaticProperty(name: std.core.String): std.reflect.StaticPropertyInfo

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.TypeInfo): Bool

    operator func !=(that: std.reflect.TypeInfo): Bool

    func hashCode: Int64

    func toString: std.core.String

    func getInstanceFunctions: Unit

    func getStaticFunctions: Unit
}

class ReflectException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class InfoNotFoundException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class MisMatchException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class IllegalSetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class IllegalTypeException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class InvocationTargetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

class GenericTypeInfo <: std.reflect.TypeInfo & std.core.Equatable<std.reflect.GenericTypeInfo> {
    operator func ==(that: std.reflect.GenericTypeInfo): Bool

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

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo>

    prop name: std.core.String

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>

    prop returnType: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func isOpen: Bool

    func isAbstract: Bool

    func apply(instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any

    func apply(instance: std.core.Any, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.InstanceFunctionInfo): Bool

    operator func !=(that: std.reflect.InstanceFunctionInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
}

class ParameterInfo <: std.core.Object & std.core.Equatable<std.reflect.ParameterInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _index: std.core.Option<Int64>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop index: Int64

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop annotations: std.core.Collection<std.core.Object>

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.ParameterInfo): Bool

    operator func !=(that: std.reflect.ParameterInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
}

enum ModifierInfo <: std.core.Equatable<std.reflect.ModifierInfo> & std.core.Hashable & std.core.ToString {
    | Open

    | Override

    | Redef

    | Abstract

    | Sealed

    | Mut

    | Static

    func byMask(mask: UInt32): std.core.Collection<std.reflect.ModifierInfo>

    func isAbstract(mask: UInt32): Bool

    func isPublic(mask: UInt32): Bool

    func isMutable(mask: UInt32): Bool

    operator func ==(that: std.reflect.ModifierInfo): Bool

    operator func !=(that: std.reflect.ModifierInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
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

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo>

    prop name: std.core.String

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>

    prop returnType: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func apply(thisType: std.reflect.TypeInfo, args: std.core.Array<std.core.Any>): std.core.Any

    func apply(thisType: std.reflect.TypeInfo, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.StaticFunctionInfo): Bool

    operator func !=(that: std.reflect.StaticFunctionInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
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

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func isOpen: Bool

    func isAbstract: Bool

    func isMutable: Bool

    func getValue(instance: std.core.Any): std.core.Any

    func setValue(instance: std.core.Any, newValue: std.core.Any): Unit

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.InstancePropertyInfo): Bool

    operator func !=(that: std.reflect.InstancePropertyInfo): Bool

    func hashCode: Int64

    func toString: std.core.String

    func setFunc(isGetter: Bool, pointer: CPointer<Unit>): Unit
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

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop modifiers: std.core.Collection<std.reflect.ModifierInfo>

    prop annotations: std.core.Collection<std.core.Object>

    func isMutable: Bool

    func getValue: std.core.Any

    func setValue(newValue: std.core.Any): Unit

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.StaticPropertyInfo): Bool

    operator func !=(that: std.reflect.StaticPropertyInfo): Bool

    func hashCode: Int64

    func toString: std.core.String

    func setFunc(isGetter: Bool, pointer: CPointer<Unit>): Unit
}

class GlobalFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalFunctionInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _name: std.core.Option<std.core.String>

    var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    var _returnType: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo>

    prop name: std.core.String

    prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>

    prop returnType: std.reflect.TypeInfo

    prop annotations: std.core.Collection<std.core.Object>

    func apply(args: std.core.Array<std.core.Any>): std.core.Any

    func apply(genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.GlobalFunctionInfo): Bool

    operator func !=(that: std.reflect.GlobalFunctionInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
}

func getFuncName(parentPointer: CPointer<Unit>): std.core.String

func getParameters(cp: CPointer<Unit>): std.collection.ArrayList<std.reflect.ParameterInfo>

func checkMethodDeclaringClassType(instance: std.core.Any, declaringTypeInfo: CPointer<Unit>): Unit

func checkParamaterSize(genericParams: std.core.Collection<std.reflect.GenericTypeInfo>, actualParamSize: Int64, genericTypeParameters: std.core.Collection<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): Unit

class InterfaceTypeInfo <: std.reflect.TypeInfo {
    var _sealedSubtypes: std.core.Option<std.core.Collection<std.reflect.TypeInfo>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    func `get`(qualifiedName: std.core.String): std.reflect.InterfaceTypeInfo

    func of(a: std.core.Any): std.reflect.InterfaceTypeInfo

    func of: std.reflect.InterfaceTypeInfo

    prop sealedSubtypes: std.core.Collection<std.reflect.TypeInfo>

    func isSealed: Bool
}

func findSubtypeFromPackage(curType: std.reflect.TypeInfo): std.core.Array<T>

func isInterface(ti: CPointer<Unit>): Bool

func isStruct(ti: CPointer<Unit>): Bool

func isPrimitive(ti: CPointer<Unit>): Bool

func isClass(ti: CPointer<Unit>): Bool

func isGeneric(ti: CPointer<Unit>): Bool

func getOrCreateTypeInfoForReflect(typeTemplate: CPointer<Unit>, typeArgs: std.core.Array<CPointer<Unit>>): CPointer<Unit>

func getTypeTemplate(name: CPointer<UInt8>): CPointer<Unit>

func checkMethodActualArgs(methodInfo: CPointer<Unit>, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>): Bool

func methodEntryPointIsNull(methodInfo: CPointer<Unit>): Bool

func isReflectUnsupportedType(ti: CPointer<Unit>): Bool

func getTypeForAny(obj: std.core.Any): CPointer<Unit>

func getTypeByMangledName: CPointer<Unit>

func getTypeName(obj: CPointer<Unit>): CPointer<UInt8>

func getTypeByQualifiedName(qualifiedName: CPointer<UInt8>): CPointer<Unit>

func getTypeQualifiedNameLength(ti: CPointer<Unit>): UInt32

func getTypeQualifiedName(ti: CPointer<Unit>, name: CPointer<UInt8>): Unit

func getNumOfInterface(ti: CPointer<Unit>): UInt32

func getInterface(ti: CPointer<Unit>, index: UInt32): CPointer<Unit>

func isSubType(ti: CPointer<Unit>, superTypeclassInfo: CPointer<Unit>): Bool

func getTypeInfoModifier(ti: CPointer<Unit>): UInt32

func getTypeInfoAnnotations(ti: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func getObjClass(obj: std.core.Object): CPointer<Unit>

func getSuperTypeInfo(ti: CPointer<Unit>): CPointer<Unit>

func getNumOfInstanceMethodInfos(ti: CPointer<Unit>): UInt32

func getInstanceMethodInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getNumOfStaticMethodInfos(ti: CPointer<Unit>): UInt32

func getStaticMethodInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getMethodName(methodInfo: CPointer<Unit>): CPointer<UInt8>

func getParameterInfo(methodInfo: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getMethodReturnType(methodInfo: CPointer<Unit>): CPointer<Unit>

func getMethodModifier(methodInfo: CPointer<Unit>): UInt32

func getMethodAnnotations(methodInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func getNumOfActualParameters(methodInfo: CPointer<Unit>): UInt32

func getNumOfGenericParameters(methodInfo: CPointer<Unit>): UInt32

func getActualParameterInfo(methodInfo: CPointer<Unit>, idx: UInt32): CPointer<Unit>

func getGenericParameterInfo(methodInfo: CPointer<Unit>, idx: UInt32): CPointer<Unit>

func getNumOfInstanceFieldInfos(ti: CPointer<Unit>): UInt32

func getInstanceFieldInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getNumOfStaticFieldInfos(ti: CPointer<Unit>): UInt32

func getStaticFieldInfo(ti: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getStaticFieldName(fieldInfo: CPointer<Unit>): CPointer<UInt8>

func getStaticFieldType(fieldInfo: CPointer<Unit>): CPointer<Unit>

func getStaticFieldAnnotations(fieldInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func getInstanceFieldName(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>): CPointer<UInt8>

func getInstanceFieldType(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>): CPointer<Unit>

func getInstanceFieldModifier(fieldInfo: CPointer<Unit>): UInt32

func getStaticFieldModifier(fieldInfo: CPointer<Unit>): UInt32

func getInstanceFieldAnnotations(fieldInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func getInstanceFieldValue(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>, instance: std.core.Any): std.core.Any

func setInstanceFieldValue(fieldInfo: CPointer<Unit>, declaringTypeInfo: CPointer<Unit>, instance: std.core.Any, value: std.core.Any): Unit

func getStaticFieldValue(fieldInfo: CPointer<Unit>): std.core.Any

func setStaticFieldValue(fieldInfo: CPointer<Unit>, value: std.core.Any): Unit

func getParameterIndex(parameterInfo: CPointer<Unit>): UInt32

func getParameterName(parameterInfo: CPointer<Unit>): CPointer<UInt8>

func getParameterType(parameterInfo: CPointer<Unit>): CPointer<Unit>

func getParameterAnnotations(parameterInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func applyCJInstanceMethod(methodInfo: CPointer<Unit>, instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any

func applyCJStaticMethod(methodInfo: CPointer<Unit>, args: std.core.Array<std.core.Any>, thisType: CPointer<Unit>): std.core.Any

func applyCJGenericInstanceMethod(methodInfo: CPointer<Unit>, instance: std.core.Any, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>): std.core.Any

func applyCJGenericStaticMethod(methodInfo: CPointer<Unit>, genericTypeArgs: std.core.Array<CPointer<Unit>>, args: std.core.Array<std.core.Any>, thisType: CPointer<Unit>): std.core.Any

func getRelatedPackageInfo(packageInfo: CPointer<Unit>): CPointer<Unit>

func getPackageName(packageInfo: CPointer<Unit>): CPointer<UInt8>

func getPackageNumOfTypeInfos(packageInfo: CPointer<Unit>): UInt32

func getPackageTypeInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getPackageNumOfGlobalMethodInfos(packageInfo: CPointer<Unit>): UInt32

func getPackageGlobalMethodInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit>

func getPackageNumOfGlobalFieldInfos(packageInfo: CPointer<Unit>): UInt32

func getPackageGlobalFieldInfo(packageInfo: CPointer<Unit>, index: UInt32): CPointer<Unit>

func loadPackage(path: CPointer<UInt8>): CPointer<Unit>

func getPackageByQualifiedName(qualifiedName: CPointer<UInt8>): CPointer<Unit>

func getPackageVersion(packageInfo: CPointer<Unit>): CPointer<UInt8>

func getSubPackages(packageInfo: CPointer<Unit>, arrayTi: CPointer<Unit>): std.core.Any

func containsModifier(source: std.core.Collection<std.reflect.ModifierInfo>, modifier: std.reflect.ModifierInfo): Bool

func getMask(modifier: std.reflect.ModifierInfo): UInt32

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

    func `get`(qualifiedName: std.core.String): std.reflect.PackageInfo

    func load(path: std.core.String): std.reflect.PackageInfo

    prop qualifiedName: std.core.String

    prop name: std.core.String

    prop noPublicInfos: std.collection.ArrayList<std.reflect.TypeInfo>

    prop typeInfos: std.core.Collection<std.reflect.TypeInfo>

    prop version: std.core.String

    prop subPackages: std.core.Collection<std.reflect.PackageInfo>

    prop parentPackage: std.reflect.PackageInfo

    prop rootPackage: std.reflect.PackageInfo

    func getSubPackage(qualifiedName: std.core.String): std.reflect.PackageInfo

    func getTypeInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.TypeInfo>): Unit

    prop variables: std.core.Collection<std.reflect.GlobalVariableInfo>

    func getGlobalVariableInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalVariableInfo>): Unit

    prop functions: std.core.Collection<std.reflect.GlobalFunctionInfo>

    func getGlobalFunctionInfos(num: UInt32, subPackageInfo: CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalFunctionInfo>): Unit

    func getTypeInfo(qualifiedTypeName: std.core.String): std.reflect.TypeInfo

    func getVariable(name: std.core.String): std.reflect.GlobalVariableInfo

    func getFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.GlobalFunctionInfo

    func getFunctions(name: std.core.String): std.core.Array<std.reflect.GlobalFunctionInfo>

    operator func ==(that: std.reflect.PackageInfo): Bool

    operator func !=(that: std.reflect.PackageInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
}

class GlobalVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalVariableInfo> & std.core.Hashable & std.core.ToString {
    let _info: CPointer<Unit>

    var _mutable: std.core.Option<Bool>

    var _name: std.core.Option<std.core.String>

    var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    prop name: std.core.String

    prop typeInfo: std.reflect.TypeInfo

    prop annotations: std.core.Collection<std.core.Object>

    func isMutable: Bool

    func getValue: std.core.Any

    func setValue(newValue: std.core.Any): Unit

    func findAnnotation: std.core.Option<T>

    operator func ==(that: std.reflect.GlobalVariableInfo): Bool

    operator func !=(that: std.reflect.GlobalVariableInfo): Bool

    func hashCode: Int64

    func toString: std.core.String
}

class PrimitiveTypeInfo <: std.reflect.TypeInfo {
    let PRIMITIVES: std.core.Array<std.core.String>

    let ARRAY_OBJECT: std.core.Array<std.core.Object>

    init(cp: CPointer<Unit>, name: std.core.String) { /* compiled code */ }

    func `get`(qualifiedName: std.core.String): std.reflect.PrimitiveTypeInfo

    func of(a: std.core.Any): std.reflect.PrimitiveTypeInfo

    func of: std.reflect.PrimitiveTypeInfo
}

func checkPrimitive(name: std.core.String): (Bool, std.core.String, std.core.String)

class StructTypeInfo <: std.reflect.TypeInfo {
    var _constructors: std.core.Option<std.core.Collection<std.reflect.ConstructorInfo>>

    var _instanceVariables: std.core.Option<std.core.Collection<std.reflect.InstanceVariableInfo>>

    var _staticVariables: std.core.Option<std.core.Collection<std.reflect.StaticVariableInfo>>

    init(cp: CPointer<Unit>) { /* compiled code */ }

    func `get`(qualifiedName: std.core.String): std.reflect.StructTypeInfo

    func of(a: std.core.Any): std.reflect.StructTypeInfo

    func of: std.reflect.StructTypeInfo

    prop constructors: std.core.Collection<std.reflect.ConstructorInfo>

    prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo>

    prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo>

    func construct(args: std.core.Array<std.core.Any>): std.core.Any

    func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo

    func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo

    func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo
}

let TYPE_INFO_CREATE_LOCK: std.sync.Mutex

class TypeTemplate {
    let _info: CPointer<Unit>

    init(info: CPointer<Unit>) { /* compiled code */ }

    func `get`(typeinfoName: std.core.String): std.reflect.TypeTemplate

    prop info: CPointer<Unit>
}

class TypeTemplateParser {
    var templates: std.collection.ArrayList<std.core.String>

    var data: std.collection.ArrayList<std.collection.ArrayList<std.core.String>>

    var map: std.collection.HashMap<std.core.String, CPointer<Unit>>

    func parse(string: std.core.String): std.reflect.TypeInfo

    func parseGenericString(name: std.core.Array<UInt8>, begin: Int64, end: Int64): Int64

    init() { /* compiled code */ }
}

type Annotation = std.core.Object

func parseParameterTypes(parameters: std.core.String): std.core.Array<std.reflect.TypeInfo>

func getByNameAndParamTypes(source: std.core.Iterable<T>, name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>, message: std.core.String, nameAndParamsGetter: (T) -> (std.core.String, std.collection.ReadOnlyList<std.reflect.ParameterInfo>)): T

func getByName(source: std.core.Iterable<T>, name: std.core.String, message: std.core.String, nameGetter: (T) -> std.core.String): T

func getTypeNameByQualifiedName(qualifiedName: std.core.String): std.core.String

func checkArgsSizeAndType(parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>, args: std.core.Array<std.core.Any>): Unit

func checkValueType(newValue: std.core.Any, typeInfo: std.reflect.TypeInfo): Unit

func findAnnotation(annotations: std.core.Collection<std.core.Object>): std.core.Option<T>

func getFieldDeclaringClassType(declaringTypeInfo: CPointer<Unit>): std.reflect.TypeInfo
