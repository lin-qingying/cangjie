// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.reflect

import std.collection.*
import std.sync.*
import std.core.*
import std.fs.*

let constructorName: std.core.String = COMPILED_CODE /* compiled code */

let FF_BYTE: UInt8 = COMPILED_CODE /* compiled code */

let NOTHING_QN: std.core.String = COMPILED_CODE /* compiled code */

let UNIT_QN: std.core.String = COMPILED_CODE /* compiled code */

let CHAR_QN: std.core.String = COMPILED_CODE /* compiled code */

let BOOL_QN: std.core.String = COMPILED_CODE /* compiled code */

let INT8_QN: std.core.String = COMPILED_CODE /* compiled code */

let INT16_QN: std.core.String = COMPILED_CODE /* compiled code */

let INT32_QN: std.core.String = COMPILED_CODE /* compiled code */

let INT64_QN: std.core.String = COMPILED_CODE /* compiled code */

let INT_NATIVE_QN: std.core.String = COMPILED_CODE /* compiled code */

let U_INT8_QN: std.core.String = COMPILED_CODE /* compiled code */

let U_INT16_QN: std.core.String = COMPILED_CODE /* compiled code */

let U_INT32_QN: std.core.String = COMPILED_CODE /* compiled code */

let U_INT64_QN: std.core.String = COMPILED_CODE /* compiled code */

let U_INT_NATIVE_QN: std.core.String = COMPILED_CODE /* compiled code */

let FLOAT16_QN: std.core.String = COMPILED_CODE /* compiled code */

let FLOAT32_QN: std.core.String = COMPILED_CODE /* compiled code */

let FLOAT64_QN: std.core.String = COMPILED_CODE /* compiled code */

let PACKAGE_LOAD_FAIL: UIntNative = COMPILED_CODE /* compiled code */

let MULTI_PACKAGE_INFO: UIntNative = COMPILED_CODE /* compiled code */

let REPEATED_PACKAGE_LOAD: UIntNative = COMPILED_CODE /* compiled code */

let REPEATED_FILENAME_LOAD: UIntNative = COMPILED_CODE /* compiled code */

let MODULE_SUFFIX: std.core.String = COMPILED_CODE /* compiled code */

let UUID_OFFSET: Int64 = COMPILED_CODE /* compiled code */

public class ClassTypeInfo <: std.reflect.TypeInfo {
    internal var _constructors: std.core.Option<std.core.Collection<std.reflect.ConstructorInfo>>

    internal var _instanceVariables: std.core.Option<std.core.Collection<std.reflect.InstanceVariableInfo>>

    internal var _staticVariables: std.core.Option<std.core.Collection<std.reflect.StaticVariableInfo>>

    internal var _superClass: std.core.Option<std.core.Option<std.reflect.ClassTypeInfo>>

    internal var _sealedSubclasses: std.core.Option<std.core.Collection<std.reflect.ClassTypeInfo>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public redef static func get(qualifiedName: std.core.String): std.reflect.ClassTypeInfo { /* compiled code */ }

    public redef static func of(a: std.core.Any): std.reflect.ClassTypeInfo { /* compiled code */ }

    public static func of(a: std.core.Object): std.reflect.ClassTypeInfo { /* compiled code */ }

    public redef static func of<T>(): std.reflect.ClassTypeInfo { /* compiled code */ }

    public prop constructors: std.core.Collection<std.reflect.ConstructorInfo> {
        get() { /* compiled code */ }
    }

    public prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo> {
        get() { /* compiled code */ }
    }

    public prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo> {
        get() { /* compiled code */ }
    }

    public prop superClass: std.core.Option<std.reflect.ClassTypeInfo> {
        get() { /* compiled code */ }
    }

    public prop sealedSubclasses: std.core.Collection<std.reflect.ClassTypeInfo> {
        get() { /* compiled code */ }
    }

    public func construct(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func isOpen(): Bool { /* compiled code */ }

    public func isAbstract(): Bool { /* compiled code */ }

    public func isSealed(): Bool { /* compiled code */ }

    public func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo { /* compiled code */ }

    public func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo { /* compiled code */ }

    public func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo { /* compiled code */ }
}

public class ConstructorInfo <: std.core.Object & std.core.Equatable<std.reflect.ConstructorInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    private var _isAbstract: std.core.Option<Bool>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(infoPointer: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func apply(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.ConstructorInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.ConstructorInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class InstanceVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.InstanceVariableInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal let _isStruct: Bool

    internal var _mutable: std.core.Option<Bool>

    internal var _name: std.core.Option<std.core.String>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(cp: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>, isStruct!: Bool = COMPILED_CODE /* compiled code */) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isMutable(): Bool { /* compiled code */ }

    public func getValue(instance: std.core.Any): std.core.Any { /* compiled code */ }

    public func setValue(instance: std.core.Any, newValue: std.core.Any): Unit { /* compiled code */ }

    internal func checkDeclaringClassType(instance: std.core.Any): Unit { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.InstanceVariableInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.InstanceVariableInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class StaticVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticVariableInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal let _isStruct: Bool

    internal var _mutable: std.core.Option<Bool>

    internal var _name: std.core.Option<std.core.String>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(cp: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isMutable(): Bool { /* compiled code */ }

    public func getValue(): std.core.Any { /* compiled code */ }

    public func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.StaticVariableInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.StaticVariableInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public abstract open class TypeInfo <: std.core.Object & std.core.Equatable<std.reflect.TypeInfo> & std.core.Hashable & std.core.ToString {
    protected let _info: std.core.CPointer<Unit>

    internal var _qualifiedName: std.core.Option<std.core.String>

    internal var _name: std.core.Option<std.core.String>

    internal var _instanceFunctions: std.core.Option<std.core.Collection<std.reflect.InstanceFunctionInfo>>

    internal var _staticFunctions: std.core.Option<std.core.Collection<std.reflect.StaticFunctionInfo>>

    internal var _instanceProperties: std.core.Option<std.core.Collection<std.reflect.InstancePropertyInfo>>

    internal var _staticProperties: std.core.Option<std.core.Collection<std.reflect.StaticPropertyInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _superInterfaces: std.core.Option<std.core.Collection<std.reflect.InterfaceTypeInfo>>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var typeTemplate: std.reflect.TypeTemplate

    internal var genericParams: std.core.Array<std.reflect.TypeInfo>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public static func of<T>(): std.reflect.TypeInfo { /* compiled code */ }

    public static func of(a: std.core.Any): std.reflect.TypeInfo { /* compiled code */ }

    @Deprecated[message: "Use 'ClassTypeInfo.of(Object)' instead."]
    public static func of(a: std.core.Object): std.reflect.ClassTypeInfo { /* compiled code */ }

    public static func get(qualifiedName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    internal static func getOrCreate(typeInfoPointer: std.core.CPointer<Unit>): std.reflect.TypeInfo { /* compiled code */ }

    internal static func createGenericTypeInfo(typeInfoPointer: std.core.CPointer<Unit>, qualifiedName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    internal static func createConcreteTypeInfo(typeInfoPointer: std.core.CPointer<Unit>, qualifiedName: std.core.String, uuid!: std.core.Option<std.core.String> = COMPILED_CODE /* compiled code */): std.reflect.TypeInfo { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop qualifiedName: std.core.String {
        get() { /* compiled code */ }
    }

    public prop instanceFunctions: std.core.Collection<std.reflect.InstanceFunctionInfo> {
        get() { /* compiled code */ }
    }

    public prop staticFunctions: std.core.Collection<std.reflect.StaticFunctionInfo> {
        get() { /* compiled code */ }
    }

    public prop instanceProperties: std.core.Collection<std.reflect.InstancePropertyInfo> {
        get() { /* compiled code */ }
    }

    public prop staticProperties: std.core.Collection<std.reflect.StaticPropertyInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public prop superInterfaces: std.core.Collection<std.reflect.InterfaceTypeInfo> {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public func isSubtypeOf(supertype: std.reflect.TypeInfo): Bool { /* compiled code */ }

    public func getInstanceFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.InstanceFunctionInfo { /* compiled code */ }

    public func getInstanceFunctions(name: std.core.String): std.core.Array<std.reflect.InstanceFunctionInfo> { /* compiled code */ }

    public func getStaticFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.StaticFunctionInfo { /* compiled code */ }

    public func getStaticFunctions(name: std.core.String): std.core.Array<std.reflect.StaticFunctionInfo> { /* compiled code */ }

    public func getInstanceProperty(name: std.core.String): std.reflect.InstancePropertyInfo { /* compiled code */ }

    public func getStaticProperty(name: std.core.String): std.reflect.StaticPropertyInfo { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.TypeInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.TypeInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func getInstanceFunctions(): Unit { /* compiled code */ }

    internal func getStaticFunctions(): Unit { /* compiled code */ }
}

public open class ReflectException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

public class InfoNotFoundException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class MisMatchException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class IllegalSetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class IllegalTypeException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class InvocationTargetException <: std.reflect.ReflectException {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public class GenericTypeInfo <: std.reflect.TypeInfo & std.core.Equatable<std.reflect.GenericTypeInfo> {
    public operator func ==(that: std.reflect.GenericTypeInfo): Bool { /* compiled code */ }

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }
}

public class InstanceFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.InstanceFunctionInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _name: std.core.Option<std.core.String>

    internal var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    internal var _returnType: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(cp: std.core.CPointer<Unit>, info: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    public prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isOpen(): Bool { /* compiled code */ }

    public func isAbstract(): Bool { /* compiled code */ }

    public func apply(instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func apply(instance: std.core.Any, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.InstanceFunctionInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.InstanceFunctionInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class ParameterInfo <: std.core.Object & std.core.Equatable<std.reflect.ParameterInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _index: std.core.Option<Int64>

    internal var _name: std.core.Option<std.core.String>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop index: Int64 {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.ParameterInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.ParameterInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public enum ModifierInfo <: std.core.Equatable<std.reflect.ModifierInfo> & std.core.Hashable & std.core.ToString {
    | Open

    | Override

    | Redef

    | Abstract

    | Sealed

    | Mut

    | Static

    internal static func byMask(mask: UInt32): std.core.Collection<std.reflect.ModifierInfo> { /* compiled code */ }

    internal static func isAbstract(mask: UInt32): Bool { /* compiled code */ }

    internal static func isPublic(mask: UInt32): Bool { /* compiled code */ }

    internal static func isMutable(mask: UInt32): Bool { /* compiled code */ }

    public override operator func ==(that: std.reflect.ModifierInfo): Bool { /* compiled code */ }

    public override operator func !=(that: std.reflect.ModifierInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public override func toString(): std.core.String { /* compiled code */ }
}

public class StaticFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticFunctionInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _name: std.core.Option<std.core.String>

    internal var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    internal var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    internal var _returnType: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(cp: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    public prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func apply(thisType: std.reflect.TypeInfo, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func apply(thisType: std.reflect.TypeInfo, genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.StaticFunctionInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.StaticFunctionInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class InstancePropertyInfo <: std.core.Object & std.core.Equatable<std.reflect.InstancePropertyInfo> & std.core.Hashable & std.core.ToString {
    internal let _name: std.core.String

    internal var _getterInfo: std.core.Option<std.reflect.InstanceFunctionInfo>

    internal var _setterInfo: std.core.Option<std.reflect.InstanceFunctionInfo>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(name: std.core.String, declaringTypeInfo: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isOpen(): Bool { /* compiled code */ }

    public func isAbstract(): Bool { /* compiled code */ }

    public func isMutable(): Bool { /* compiled code */ }

    public func getValue(instance: std.core.Any): std.core.Any { /* compiled code */ }

    public func setValue(instance: std.core.Any, newValue: std.core.Any): Unit { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.InstancePropertyInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.InstancePropertyInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func setFunc(isGetter: Bool, pointer: std.core.CPointer<Unit>): Unit { /* compiled code */ }
}

public class StaticPropertyInfo <: std.core.Object & std.core.Equatable<std.reflect.StaticPropertyInfo> & std.core.Hashable & std.core.ToString {
    internal let _name: std.core.String

    internal var _getterInfo: std.core.Option<std.reflect.StaticFunctionInfo>

    internal var _setterInfo: std.core.Option<std.reflect.StaticFunctionInfo>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _modifiers: std.core.Option<std.core.Collection<std.reflect.ModifierInfo>>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    internal var _declaringTypeInfo: std.core.CPointer<Unit>

    init(name: std.core.String, declaringTypeInfo: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop modifiers: std.core.Collection<std.reflect.ModifierInfo> {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isMutable(): Bool { /* compiled code */ }

    public func getValue(): std.core.Any { /* compiled code */ }

    public func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.StaticPropertyInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.StaticPropertyInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }

    internal func setFunc(isGetter: Bool, pointer: std.core.CPointer<Unit>): Unit { /* compiled code */ }
}

public class GlobalFunctionInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalFunctionInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _name: std.core.Option<std.core.String>

    internal var _parameters: std.core.Option<std.collection.ArrayList<std.reflect.ParameterInfo>>

    internal var _genericParams: std.core.Option<std.core.Collection<std.reflect.GenericTypeInfo>>

    internal var _returnType: std.core.Option<std.reflect.TypeInfo>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop genericParams: std.core.Collection<std.reflect.GenericTypeInfo> {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo> {
        get() { /* compiled code */ }
    }

    public prop returnType: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func apply(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func apply(genericTypeArgs: std.core.Array<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.GlobalFunctionInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.GlobalFunctionInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

internal func getFuncName(parentPointer: std.core.CPointer<Unit>): std.core.String { /* compiled code */ }

internal func getParameters(cp: std.core.CPointer<Unit>): std.collection.ArrayList<std.reflect.ParameterInfo> { /* compiled code */ }

internal func checkMethodDeclaringClassType(instance: std.core.Any, declaringTypeInfo: std.core.CPointer<Unit>): Unit { /* compiled code */ }

internal func checkParamaterSize(genericParams: std.core.Collection<std.reflect.GenericTypeInfo>, actualParamSize: Int64, genericTypeParameters: std.core.Collection<std.reflect.TypeInfo>, args: std.core.Array<std.core.Any>): Unit { /* compiled code */ }

public class InterfaceTypeInfo <: std.reflect.TypeInfo {
    internal var _sealedSubtypes: std.core.Option<std.core.Collection<std.reflect.TypeInfo>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public redef static func get(qualifiedName: std.core.String): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    public redef static func of(a: std.core.Any): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    public redef static func of<T>(): std.reflect.InterfaceTypeInfo { /* compiled code */ }

    public prop sealedSubtypes: std.core.Collection<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    public func isSealed(): Bool { /* compiled code */ }
}

internal func findSubtypeFromPackage<T>(curType: std.reflect.TypeInfo): std.core.Array<T> { /* compiled code */ }

@Intrinsic
internal func isInterface(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func isStruct(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func isPrimitive(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func isClass(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func isGeneric(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func getOrCreateTypeInfoForReflect(typeTemplate: std.core.CPointer<Unit>, typeArgs: std.core.Array<std.core.CPointer<Unit>>): std.core.CPointer<Unit>

@Intrinsic
internal func getTypeTemplate(name: std.core.CPointer<UInt8>): std.core.CPointer<Unit>

@Intrinsic
internal func checkMethodActualArgs(methodInfo: std.core.CPointer<Unit>, genericTypeArgs: std.core.Array<std.core.CPointer<Unit>>, args: std.core.Array<std.core.Any>): Bool

@Intrinsic
internal func methodEntryPointIsNull(methodInfo: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func isReflectUnsupportedType(ti: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func getTypeForAny(obj: std.core.Any): std.core.CPointer<Unit>

@Intrinsic
internal func getTypeByMangledName<T>(): std.core.CPointer<Unit>

@Intrinsic
internal func getTypeName(obj: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getTypeByQualifiedName(qualifiedName: std.core.CPointer<UInt8>): std.core.CPointer<Unit>

@Intrinsic
internal func getTypeQualifiedNameLength(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getTypeQualifiedName(ti: std.core.CPointer<Unit>, name: std.core.CPointer<UInt8>): Unit

@Intrinsic
internal func getNumOfInterface(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getInterface(ti: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func isSubType(ti: std.core.CPointer<Unit>, superTypeclassInfo: std.core.CPointer<Unit>): Bool

@Intrinsic
internal func getTypeInfoModifier(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getTypeInfoAnnotations(ti: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func getObjClass(obj: std.core.Object): std.core.CPointer<Unit>

@Intrinsic
internal func getSuperTypeInfo(ti: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getNumOfInstanceMethodInfos(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getInstanceMethodInfo(ti: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getNumOfStaticMethodInfos(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getStaticMethodInfo(ti: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getMethodName(methodInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getParameterInfo(methodInfo: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getMethodReturnType(methodInfo: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getMethodModifier(methodInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getMethodAnnotations(methodInfo: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func getNumOfActualParameters(methodInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getNumOfGenericParameters(methodInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getActualParameterInfo(methodInfo: std.core.CPointer<Unit>, idx: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getGenericParameterInfo(methodInfo: std.core.CPointer<Unit>, idx: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getNumOfInstanceFieldInfos(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getInstanceFieldInfo(ti: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getNumOfStaticFieldInfos(ti: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getStaticFieldInfo(ti: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getStaticFieldName(fieldInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getStaticFieldType(fieldInfo: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getStaticFieldAnnotations(fieldInfo: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func getInstanceFieldName(fieldInfo: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getInstanceFieldType(fieldInfo: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getInstanceFieldModifier(fieldInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getStaticFieldModifier(fieldInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getInstanceFieldAnnotations(fieldInfo: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func getInstanceFieldValue(fieldInfo: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>, instance: std.core.Any): std.core.Any

@Intrinsic
internal func setInstanceFieldValue(fieldInfo: std.core.CPointer<Unit>, declaringTypeInfo: std.core.CPointer<Unit>, instance: std.core.Any, value: std.core.Any): Unit

@Intrinsic
internal func getStaticFieldValue(fieldInfo: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func setStaticFieldValue(fieldInfo: std.core.CPointer<Unit>, value: std.core.Any): Unit

@Intrinsic
internal func getParameterIndex(parameterInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getParameterName(parameterInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getParameterType(parameterInfo: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getParameterAnnotations(parameterInfo: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func applyCJInstanceMethod(methodInfo: std.core.CPointer<Unit>, instance: std.core.Any, args: std.core.Array<std.core.Any>): std.core.Any

@Intrinsic
internal func applyCJStaticMethod(methodInfo: std.core.CPointer<Unit>, args: std.core.Array<std.core.Any>, thisType: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func applyCJGenericInstanceMethod(methodInfo: std.core.CPointer<Unit>, instance: std.core.Any, genericTypeArgs: std.core.Array<std.core.CPointer<Unit>>, args: std.core.Array<std.core.Any>): std.core.Any

@Intrinsic
internal func applyCJGenericStaticMethod(methodInfo: std.core.CPointer<Unit>, genericTypeArgs: std.core.Array<std.core.CPointer<Unit>>, args: std.core.Array<std.core.Any>, thisType: std.core.CPointer<Unit>): std.core.Any

@Intrinsic
internal func getRelatedPackageInfo(packageInfo: std.core.CPointer<Unit>): std.core.CPointer<Unit>

@Intrinsic
internal func getPackageName(packageInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getPackageNumOfTypeInfos(packageInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getPackageTypeInfo(packageInfo: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getPackageNumOfGlobalMethodInfos(packageInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getPackageGlobalMethodInfo(packageInfo: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func getPackageNumOfGlobalFieldInfos(packageInfo: std.core.CPointer<Unit>): UInt32

@Intrinsic
internal func getPackageGlobalFieldInfo(packageInfo: std.core.CPointer<Unit>, index: UInt32): std.core.CPointer<Unit>

@Intrinsic
internal func loadPackage(path: std.core.CPointer<UInt8>): std.core.CPointer<Unit>

@Intrinsic
internal func getPackageByQualifiedName(qualifiedName: std.core.CPointer<UInt8>): std.core.CPointer<Unit>

@Intrinsic
internal func getPackageVersion(packageInfo: std.core.CPointer<Unit>): std.core.CPointer<UInt8>

@Intrinsic
internal func getSubPackages(packageInfo: std.core.CPointer<Unit>, arrayTi: std.core.CPointer<Unit>): std.core.Any

internal func containsModifier(source: std.core.Collection<std.reflect.ModifierInfo>, modifier: std.reflect.ModifierInfo): Bool { /* compiled code */ }

internal func getMask(modifier: std.reflect.ModifierInfo): UInt32 { /* compiled code */ }

public class PackageInfo <: std.core.Object & std.core.Equatable<std.reflect.PackageInfo> & std.core.Hashable & std.core.ToString {
    internal let _packageInfo: std.core.CPointer<Unit>

    internal var _qualifiedName: std.core.Option<std.core.String>

    internal var _name: std.core.Option<std.core.String>

    internal var _typeInfos: std.core.Option<std.core.Collection<std.reflect.TypeInfo>>

    internal var _variables: std.core.Option<std.core.Collection<std.reflect.GlobalVariableInfo>>

    internal var _functions: std.core.Option<std.core.Collection<std.reflect.GlobalFunctionInfo>>

    internal var _version: std.core.Option<std.core.String>

    internal var _rootPackage: std.core.Option<std.reflect.PackageInfo>

    internal var _noPublicInfoList: std.collection.ArrayList<std.reflect.TypeInfo>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public static func get(qualifiedName: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    public static func load(path: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    public prop qualifiedName: std.core.String {
        get() { /* compiled code */ }
    }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    internal prop noPublicInfos: std.collection.ArrayList<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    public prop typeInfos: std.core.Collection<std.reflect.TypeInfo> {
        get() { /* compiled code */ }
    }

    public prop version: std.core.String {
        get() { /* compiled code */ }
    }

    public prop subPackages: std.core.Collection<std.reflect.PackageInfo> {
        get() { /* compiled code */ }
    }

    public prop parentPackage: std.reflect.PackageInfo {
        get() { /* compiled code */ }
    }

    public prop rootPackage: std.reflect.PackageInfo {
        get() { /* compiled code */ }
    }

    public func getSubPackage(qualifiedName: std.core.String): std.reflect.PackageInfo { /* compiled code */ }

    internal func getTypeInfos(num: UInt32, subPackageInfo: std.core.CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.TypeInfo>): Unit { /* compiled code */ }

    public prop variables: std.core.Collection<std.reflect.GlobalVariableInfo> {
        get() { /* compiled code */ }
    }

    internal func getGlobalVariableInfos(num: UInt32, subPackageInfo: std.core.CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalVariableInfo>): Unit { /* compiled code */ }

    public prop functions: std.core.Collection<std.reflect.GlobalFunctionInfo> {
        get() { /* compiled code */ }
    }

    internal func getGlobalFunctionInfos(num: UInt32, subPackageInfo: std.core.CPointer<Unit>, infoList: std.collection.ArrayList<std.reflect.GlobalFunctionInfo>): Unit { /* compiled code */ }

    public func getTypeInfo(qualifiedTypeName: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    public func getVariable(name: std.core.String): std.reflect.GlobalVariableInfo { /* compiled code */ }

    public func getFunction(name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.GlobalFunctionInfo { /* compiled code */ }

    public func getFunctions(name: std.core.String): std.core.Array<std.reflect.GlobalFunctionInfo> { /* compiled code */ }

    public operator func ==(that: std.reflect.PackageInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.PackageInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class GlobalVariableInfo <: std.core.Object & std.core.Equatable<std.reflect.GlobalVariableInfo> & std.core.Hashable & std.core.ToString {
    internal let _info: std.core.CPointer<Unit>

    internal var _mutable: std.core.Option<Bool>

    internal var _name: std.core.Option<std.core.String>

    internal var _typeInfo: std.core.Option<std.reflect.TypeInfo>

    internal var _annotations: std.core.Option<std.core.Collection<std.core.Object>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public prop name: std.core.String {
        get() { /* compiled code */ }
    }

    public prop typeInfo: std.reflect.TypeInfo {
        get() { /* compiled code */ }
    }

    public prop annotations: std.core.Collection<std.core.Object> {
        get() { /* compiled code */ }
    }

    public func isMutable(): Bool { /* compiled code */ }

    public func getValue(): std.core.Any { /* compiled code */ }

    public func setValue(newValue: std.core.Any): Unit { /* compiled code */ }

    public func findAnnotation<T>(): std.core.Option<T> { /* compiled code */ }

    public operator func ==(that: std.reflect.GlobalVariableInfo): Bool { /* compiled code */ }

    public operator func !=(that: std.reflect.GlobalVariableInfo): Bool { /* compiled code */ }

    public func hashCode(): Int64 { /* compiled code */ }

    public func toString(): std.core.String { /* compiled code */ }
}

public class PrimitiveTypeInfo <: std.reflect.TypeInfo {
    internal static let PRIMITIVES: std.core.Array<std.core.String>

    internal static let ARRAY_OBJECT: std.core.Array<std.core.Object>

    init(cp: std.core.CPointer<Unit>, name: std.core.String) { /* compiled code */ }

    public redef static func get(qualifiedName: std.core.String): std.reflect.PrimitiveTypeInfo { /* compiled code */ }

    public redef static func of(a: std.core.Any): std.reflect.PrimitiveTypeInfo { /* compiled code */ }

    public redef static func of<T>(): std.reflect.PrimitiveTypeInfo { /* compiled code */ }
}

internal func checkPrimitive(name: std.core.String): (Bool, std.core.String, std.core.String) { /* compiled code */ }

public class StructTypeInfo <: std.reflect.TypeInfo {
    internal var _constructors: std.core.Option<std.core.Collection<std.reflect.ConstructorInfo>>

    internal var _instanceVariables: std.core.Option<std.core.Collection<std.reflect.InstanceVariableInfo>>

    internal var _staticVariables: std.core.Option<std.core.Collection<std.reflect.StaticVariableInfo>>

    init(cp: std.core.CPointer<Unit>) { /* compiled code */ }

    public redef static func get(qualifiedName: std.core.String): std.reflect.StructTypeInfo { /* compiled code */ }

    public redef static func of(a: std.core.Any): std.reflect.StructTypeInfo { /* compiled code */ }

    public redef static func of<T>(): std.reflect.StructTypeInfo { /* compiled code */ }

    public prop constructors: std.core.Collection<std.reflect.ConstructorInfo> {
        get() { /* compiled code */ }
    }

    public prop instanceVariables: std.core.Collection<std.reflect.InstanceVariableInfo> {
        get() { /* compiled code */ }
    }

    public prop staticVariables: std.core.Collection<std.reflect.StaticVariableInfo> {
        get() { /* compiled code */ }
    }

    public func construct(args: std.core.Array<std.core.Any>): std.core.Any { /* compiled code */ }

    public func getConstructor(parameterTypes: std.core.Array<std.reflect.TypeInfo>): std.reflect.ConstructorInfo { /* compiled code */ }

    public func getInstanceVariable(name: std.core.String): std.reflect.InstanceVariableInfo { /* compiled code */ }

    public func getStaticVariable(name: std.core.String): std.reflect.StaticVariableInfo { /* compiled code */ }
}

let TYPE_INFO_CREATE_LOCK: std.sync.Mutex

internal class TypeTemplate {
    protected let _info: std.core.CPointer<Unit>

    init(info: std.core.CPointer<Unit>) { /* compiled code */ }

    internal static func get(typeinfoName: std.core.String): std.reflect.TypeTemplate { /* compiled code */ }

    internal prop info: std.core.CPointer<Unit> {
        get() { /* compiled code */ }
    }
}

internal class TypeTemplateParser {
    internal var templates: std.collection.ArrayList<std.core.String>

    internal var data: std.collection.ArrayList<std.collection.ArrayList<std.core.String>>

    internal var map: std.collection.HashMap<std.core.String, std.core.CPointer<Unit>>

    internal func parse(string: std.core.String): std.reflect.TypeInfo { /* compiled code */ }

    internal func parseGenericString(name: std.core.Array<UInt8>, begin: Int64, end: Int64): Int64 { /* compiled code */ }

    init() { /* compiled code */ }
}

public type Annotation = std.core.Object

public func parseParameterTypes(parameters: std.core.String): std.core.Array<std.reflect.TypeInfo> { /* compiled code */ }

internal func getByNameAndParamTypes<T>(source: std.core.Iterable<T>, name: std.core.String, parameterTypes: std.core.Array<std.reflect.TypeInfo>, message: std.core.String, nameAndParamsGetter: (T) -> (std.core.String, std.collection.ReadOnlyList<std.reflect.ParameterInfo>)): T { /* compiled code */ }

internal func getByName<T>(source: std.core.Iterable<T>, name: std.core.String, message: std.core.String, nameGetter: (T) -> std.core.String): T { /* compiled code */ }

internal func getTypeNameByQualifiedName(qualifiedName: std.core.String): std.core.String { /* compiled code */ }

internal func checkArgsSizeAndType(parameters: std.collection.ReadOnlyList<std.reflect.ParameterInfo>, args: std.core.Array<std.core.Any>): Unit { /* compiled code */ }

internal func checkValueType(newValue: std.core.Any, typeInfo: std.reflect.TypeInfo): Unit { /* compiled code */ }

internal func findAnnotation<T>(annotations: std.core.Collection<std.core.Object>): std.core.Option<T> { /* compiled code */ }

internal func getFieldDeclaringClassType(declaringTypeInfo: std.core.CPointer<Unit>): std.reflect.TypeInfo { /* compiled code */ }
