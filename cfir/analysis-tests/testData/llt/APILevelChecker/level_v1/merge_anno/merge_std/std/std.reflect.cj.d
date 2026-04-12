// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.reflect
internal import std.collection.{
    ArrayList,
    HashMap,
    HashSet
}
internal import std.sync.*
internal import std.fs.*

internal import std.collection.{
    ArrayList,
    HashMap
}
internal import std.sync.ReentrantMutex
import std.collection.*
import std.fs.*
import std.sync.*
internal import std.collection.{
    ArrayList,
    HashMap,
    ReadOnlyList
}
internal import std.collection.ReadOnlyList
import std.collection.{
    ArrayList,
    HashMap
}
import std.sync.ReentrantMutex

@!APILevel[12, atomicservice : true]
public class ClassTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public static func of(a: Object): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): ClassTypeInfo
    
    /**
    * Returns the collection of public constructors of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop constructors: Collection<ConstructorInfo>
    
    /**
    * Returns the collection of public instance variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop instanceVariables: Collection<InstanceVariableInfo>
    
    /**
    * Returns the collection of public static variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop staticVariables: Collection<StaticVariableInfo>
    
    /**
    * Returns the super class of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop superClass: Option<ClassTypeInfo>
    
    /**
    * Returns the collection of sealed subclasses of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop sealedSubclasses: Collection<ClassTypeInfo>
    
    /**
    * Creates a new instance of class with corresponding type for incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalTypeException, if this class ia abstract.
    * @throw MisMatchException, if no public constructor is found to construct new instance.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called to construct the instance.
    */
    @!APILevel[12, atomicservice : true]
    public func construct(args: Array<Any>): Any
    
    /**
    * Returns true if type info is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if type info is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Returns true if type info is 'sealed', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isSealed(): Bool
    
    /**
    * Searches the type info's public constructor by incoming parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getConstructor(parameterTypes: Array<TypeInfo>): ConstructorInfo
    
    /**
    * Searches the type info's public instance variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceVariable(name: String): InstanceVariableInfo
    
    /**
    * Searches the type info's public static variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticVariable(name: String): StaticVariableInfo
}

/**
* Contains reflective information about classes.
*/
@!APILevel[12, atomicservice : true]
public class ClassTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public static func of(a: Object): ClassTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): ClassTypeInfo
    
    /**
    * Returns the collection of public constructors of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop constructors: Collection<ConstructorInfo>
    
    /**
    * Returns the collection of public instance variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop instanceVariables: Collection<InstanceVariableInfo>
    
    /**
    * Returns the collection of public static variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop staticVariables: Collection<StaticVariableInfo>
    
    /**
    * Returns the super class of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop superClass: Option<ClassTypeInfo>
    
    /**
    * Returns the collection of sealed subclasses of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop sealedSubclasses: Collection<ClassTypeInfo>
    
    /**
    * Creates a new instance of class with corresponding type for incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalTypeException, if this class ia abstract.
    * @throw MisMatchException, if no public constructor is found to construct new instance.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called to construct the instance.
    */
    @!APILevel[12, atomicservice : true]
    public func construct(args: Array<Any>): Any
    
    /**
    * Returns true if type info is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if type info is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Returns true if type info is 'sealed', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isSealed(): Bool
    
    /**
    * Searches the type info's public constructor by incoming parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getConstructor(parameterTypes: Array<TypeInfo>): ConstructorInfo
    
    /**
    * Searches the type info's public instance variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceVariable(name: String): InstanceVariableInfo
    
    /**
    * Searches the type info's public static variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticVariable(name: String): StaticVariableInfo
}

@!APILevel[12, atomicservice : true]
public open class ReflectException  <: Exception {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class InfoNotFoundException <: ReflectException {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class MisMatchException <: ReflectException {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class IllegalSetException <: ReflectException {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class IllegalTypeException <: ReflectException {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class InvocationTargetException <: ReflectException {
    @!APILevel[12, atomicservice : true]
    public init()
    
    @!APILevel[12, atomicservice : true]
    public init(message: String)
}

@!APILevel[12, atomicservice : true]
public class GenericTypeInfo <: TypeInfo & Equatable<GenericTypeInfo> {
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: GenericTypeInfo): Bool
}

/**
* Contains the reflective information about instance functions.
*/
@!APILevel[12, atomicservice : true]
public class InstanceFunctionInfo <: Equatable<InstanceFunctionInfo> & Hashable & ToString {
    @!APILevel[12, atomicservice : true]
    public prop genericParams: Collection<GenericTypeInfo>
    
    /**
    * Returns the name of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the list of parameters of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the return type of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance function is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if instance function is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Makes a call of public instance function with incoming args for incoming instance.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if an exception is thrown when the function is abstract.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(instance: Any, args: Array<Any>): Any
    
    @!APILevel[12, atomicservice : true]
    public func apply(instance: Any, genericTypeArgs: Array<TypeInfo>, args: Array<Any>): Any
    
    /**
    * Searches the instance function's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstanceFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstanceFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static functions.
*/
@!APILevel[12, atomicservice : true]
public class StaticFunctionInfo <: Equatable<StaticFunctionInfo> & Hashable & ToString {
    @!APILevel[12, atomicservice : true]
    public prop genericParams: Collection<GenericTypeInfo>
    
    /**
    * Returns the name of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the list of parameters of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the return type of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    /**
    * Returns the collection of modifiers of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of static function with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(thisType: TypeInfo, args: Array<Any>): Any
    
    @!APILevel[12, atomicservice : true]
    public func apply(thisType: TypeInfo, genericTypeArgs: Array<TypeInfo>, args: Array<Any>): Any
    
    /**
    * Searches the static function's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about instance properties.
*/
@!APILevel[12, atomicservice : true]
public class InstancePropertyInfo <: Equatable<InstancePropertyInfo> & Hashable & ToString {
    /**
    * Returns the name of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance property is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if instance property is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Returns true if instance property is 'mut' and typeInfo is not struct
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the instance property's value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(instance: Any): Any
    
    /**
    * Updates the instance property's value with incoming new value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(instance: Any, newValue: Any): Unit
    
    /**
    * Searches the instance property's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstancePropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstancePropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static properties.
*/
@!APILevel[12, atomicservice : true]
public class StaticPropertyInfo <: Equatable<StaticPropertyInfo> & Hashable & ToString {
    /**
    * Returns the name of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if static property is 'mut' and typeInfo is not struct
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the instance property's value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the instance property's value with incoming new value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    /**
    * Searches the static property's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticPropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticPropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about constructors.
*/
@!APILevel[12, atomicservice : true]
public class ConstructorInfo <: Equatable<ConstructorInfo> & Hashable & ToString {
    /**
    * Returns the list of parameters of constructor.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the collection of annotations of constructor.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of constructor with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if the constructor is abstract
    */
    @!APILevel[12, atomicservice : true]
    public func apply(args: Array<Any>): Any
    
    /**
    * Searches the constructor's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ConstructorInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ConstructorInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about global functions.
*/
@!APILevel[12, atomicservice : true]
public class GlobalFunctionInfo <: Equatable<GlobalFunctionInfo> & Hashable & ToString {
    @!APILevel[12, atomicservice : true]
    public prop genericParams: Collection<GenericTypeInfo>
    
    // Function name, overloaded functions have the same function name
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the list of parameters of global function.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the return type of global function.
    */
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    // Annotation, when there is no data, size is 0. Note that this api does not guarantee a constant traversal order.
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of global function with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(args: Array<Any>): Any
    
    @!APILevel[12, atomicservice : true]
    public func apply(genericTypeArgs: Array<TypeInfo>, args: Array<Any>): Any
    
    // Get the annotation by name, return None if not found
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: GlobalFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: GlobalFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about instance functions.
*/
@!APILevel[12, atomicservice : true]
public class InstanceFunctionInfo <: Equatable<InstanceFunctionInfo> & Hashable & ToString {
    /**
    * Returns the name of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the list of parameters of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the return type of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance function.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance function is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if instance function is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Makes a call of public instance function with incoming args for incoming instance.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called this instance function.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(instance: Any, args: Array<Any>): Any
    
    /**
    * Searches the instance function's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstanceFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstanceFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static functions.
*/
@!APILevel[12, atomicservice : true]
public class StaticFunctionInfo <: Equatable<StaticFunctionInfo> & Hashable & ToString {
    /**
    * Returns the name of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the list of parameters of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the return type of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    /**
    * Returns the collection of modifiers of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static function.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of static function with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called of this static function.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(args: Array<Any>): Any
    
    /**
    * Searches the static function's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about constructors.
*/
@!APILevel[12, atomicservice : true]
public class ConstructorInfo <: Equatable<ConstructorInfo> & Hashable & ToString {
    /**
    * Returns the list of parameters of constructor.
    */
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    /**
    * Returns the collection of annotations of constructor.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of constructor with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called to construct the instance.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(args: Array<Any>): Any
    
    /**
    * Searches the constructor's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ConstructorInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ConstructorInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about global functions.
*/
@!APILevel[12, atomicservice : true]
public class GlobalFunctionInfo <: Equatable<GlobalFunctionInfo> & Hashable & ToString {
    // Function name, overloaded functions have the same function name
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    // parameter list
    @!APILevel[12, atomicservice : true]
    public prop parameters: ReadOnlyList<ParameterInfo>
    
    // return type
    @!APILevel[12, atomicservice : true]
    public prop returnType: TypeInfo
    
    // Annotation, when there is no data, size is 0. Note that this api does not guarantee a constant traversal order.
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Makes a call of global function with incoming args.
    *
    * @param args parameter list.
    *
    * @throw IllegalArgumentException, if the number of input parameters is different from the number of invoked function parameters.
    * @throw IllegalTypeException, if the input parameter type does not match the required parameter type.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called of this global function.
    */
    @!APILevel[12, atomicservice : true]
    public func apply(args: Array<Any>): Any
    
    // Get the annotation by name, return None if not found
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: GlobalFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: GlobalFunctionInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains reflective information about interfaces.
*/
@!APILevel[12, atomicservice : true]
public class InterfaceTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): InterfaceTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): InterfaceTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): InterfaceTypeInfo
    
    /**
    * Returns the collection of sealed subtypes of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop sealedSubtypes: Collection<TypeInfo>
    
    @!APILevel[12, atomicservice : true]
    public func isSealed(): Bool
}

/**
* Contains reflective information about interfaces.
*/
@!APILevel[12, atomicservice : true]
public class InterfaceTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): InterfaceTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): InterfaceTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): InterfaceTypeInfo
    
    /**
    * Returns the collection of sealed subtypes of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop sealedSubtypes: Collection<TypeInfo>
}

/**
* Contains the reflective information about modifiers.
*/
@!APILevel[12, atomicservice : true]
public enum ModifierInfo <: Equatable<ModifierInfo> & Hashable & ToString  {
    @!APILevel[12, atomicservice : true]
    Open |
    @!APILevel[12, atomicservice : true]
    Override |
    @!APILevel[12, atomicservice : true]
    Redef |
    @!APILevel[12, atomicservice : true]
    Abstract |
    @!APILevel[12, atomicservice : true]
    Sealed |
    @!APILevel[12, atomicservice : true]
    Mut |
    @!APILevel[12, atomicservice : true]
    Static
    @!APILevel[12, atomicservice : true]
    public override operator func == (that: ModifierInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public override operator func != (that: ModifierInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public override func toString(): String
}

/**
* Contains the reflective information about modules.
*/
@!APILevel[12, atomicservice : true]
public class ModuleInfo <: Equatable<ModuleInfo> & Hashable & ToString {
    // Basic module information
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    @!APILevel[12, atomicservice : true]
    public prop version: String
    
    @!APILevel[12, atomicservice : true]
    public prop packages: Collection<PackageInfo>
    
    // Load a Cangjie dynamic module with a specified path.// path does not need to contain the file extension, the file format supported by path is determined by the backend implementation.// The load function has a cache function, and the dynamic module information that has been loaded will be cached// When repeatedly loading a dynamic module with different paths but the same module name, if the module summary (module name + module version number)//  in the binary files of the two paths is the same, no exception will be thrown,//  and the already loaded one will always be returned//  A dynamic module of the same name. Otherwise an exception will be thrown.// If the loading fails, an exception will be thrown. The failures include:// - could not find the file// - file does not meet format requirements or is not compatible// - the dynamic module's dependency does not exist or is incompatible
    @!APILevel[12, atomicservice : true]
    public static func load(path: String): ModuleInfo
    
    // Return a specified moduleInfo from the loaded dynamic module// return None if not found
    @!APILevel[12, atomicservice : true]
    public static func find(moduleName: String): Option<ModuleInfo>
    
    // Return a specified packageInfo from the module// return None if not found
    @!APILevel[12, atomicservice : true]
    public func getPackageInfo(packageName: String): PackageInfo
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ModuleInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ModuleInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about packages.
*/
@!APILevel[12, atomicservice : true]
public class PackageInfo <: Equatable<PackageInfo> & Hashable & ToString {
    // Return a specified packageInfo from the loaded dynamic module
    @!APILevel[12, atomicservice : true]
    public static func get(qualifiedName: String): PackageInfo
    
    @!APILevel[12, atomicservice : true]
    public static func load(path: String): PackageInfo
    
    // package name with prefix// returns "a.b.c" when the package name is "a.b.c"
    @!APILevel[12, atomicservice : true]
    public prop qualifiedName: String
    
    // current package name// returns "c" when the package name is "a.b.c"
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    // Get all type information of the current package
    @!APILevel[12, atomicservice : true]
    public prop typeInfos: Collection<TypeInfo>
    
    @!APILevel[12, atomicservice : true]
    public prop version: String
    
    @!APILevel[12, atomicservice : true]
    public prop subPackages: Collection<PackageInfo>
    
    @!APILevel[12, atomicservice : true]
    public prop parentPackage: PackageInfo
    
    @!APILevel[12, atomicservice : true]
    public prop rootPackage: PackageInfo
    
    @!APILevel[12, atomicservice : true]
    public func getSubPackage(qualifiedName: String): PackageInfo
    
    // Get all global variable information of the current package
    @!APILevel[12, atomicservice : true]
    public prop variables: Collection<GlobalVariableInfo>
    
    // Get all global function information of the current package
    @!APILevel[12, atomicservice : true]
    public prop functions: Collection<GlobalFunctionInfo>
    
    // lookup type information, return None if// Unable to look up the generic type
    @!APILevel[12, atomicservice : true]
    public func getTypeInfo(qualifiedTypeName: String): TypeInfo
    
    // Find the global variable, return None if not found or the type does not match
    @!APILevel[12, atomicservice : true]
    public func getVariable(name: String): GlobalVariableInfo
    
    // Find the global function, return None if not found or the type does not match// Could not find generic function
    @!APILevel[12, atomicservice : true]
    public func getFunction(name: String, parameterTypes: Array<TypeInfo>): GlobalFunctionInfo
    
    @!APILevel[12, atomicservice : true]
    public func getFunctions(name: String): Array<GlobalFunctionInfo>
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: PackageInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: PackageInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about packages.
*/
@!APILevel[12, atomicservice : true]
public class PackageInfo <: Equatable<PackageInfo> & Hashable & ToString {
    // Return a specified packageInfo from the loaded dynamic module// return None if not found
    @!APILevel[12, atomicservice : true]
    public static func get(qualifiedName: String): PackageInfo
    
    // package name with prefix// returns "a.b.c" when the package name is "a.b.c"
    @!APILevel[12, atomicservice : true]
    public prop qualifiedName: String
    
    // current package name// returns "c" when the package name is "a.b.c"
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    // Get all type information of the current package
    @!APILevel[12, atomicservice : true]
    public prop typeInfos: Collection<TypeInfo>
    
    // Get all global variable information of the current package
    @!APILevel[12, atomicservice : true]
    public prop variables: Collection<GlobalVariableInfo>
    
    // Get all global function information of the current package
    @!APILevel[12, atomicservice : true]
    public prop functions: Collection<GlobalFunctionInfo>
    
    // lookup type information, return None if// Unable to look up the generic type
    @!APILevel[12, atomicservice : true]
    public func getTypeInfo(name: String): TypeInfo
    
    // Find the global variable, return None if not found or the type does not match
    @!APILevel[12, atomicservice : true]
    public func getVariable(name: String): GlobalVariableInfo
    
    // Find the global function, return None if not found or the type does not match// Could not find generic function
    @!APILevel[12, atomicservice : true]
    public func getFunction(name: String, parameterTypes: Array<TypeInfo>): GlobalFunctionInfo
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: PackageInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: PackageInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about parameters.
*/
@!APILevel[12, atomicservice : true]
public class ParameterInfo <: Equatable<ParameterInfo> & Hashable & ToString {
    /**
    * Returns the index of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop index: Int64
    
    /**
    * Returns the name of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of annotations of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Searches the parameter's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ParameterInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ParameterInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about parameters.
*/
@!APILevel[12, atomicservice : true]
public class ParameterInfo <: Equatable<ParameterInfo> & Hashable & ToString {
    /**
    * Returns the index of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop index: Int64
    
    /**
    * Returns the name of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of annotations of parameter.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Searches the parameter's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: ParameterInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: ParameterInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains reflective information about primitive types.
*/
@!APILevel[12, atomicservice : true]
public class PrimitiveTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): PrimitiveTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): PrimitiveTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): PrimitiveTypeInfo
}

/**
* Contains reflective information about primitive types.
*/
@!APILevel[12, atomicservice : true]
public class PrimitiveTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): PrimitiveTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): PrimitiveTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): PrimitiveTypeInfo
}

/**
* Contains the reflective information about instance properties.
*/
@!APILevel[12, atomicservice : true]
public class InstancePropertyInfo <: Equatable<InstancePropertyInfo> & Hashable & ToString {
    /**
    * Returns the name of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance property.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance property is 'public', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isOpen(): Bool
    
    /**
    * Returns true if instance property is 'abstract', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isAbstract(): Bool
    
    /**
    * Returns true if instance property is 'mut', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the instance property's value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(instance: Any): Any
    
    /**
    * Updates the instance property's value with incoming new value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(instance: Any, newValue: Any): Unit
    
    /**
    * Searches the instance property's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstancePropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstancePropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static properties.
*/
@!APILevel[12, atomicservice : true]
public class StaticPropertyInfo <: Equatable<StaticPropertyInfo> & Hashable & ToString {
    /**
    * Returns the name of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static property.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if static property is 'mut', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the static property's value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the static property's value with incoming new value.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    /**
    * Searches the static function's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticPropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticPropertyInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains reflective information about structs.
*/
@!APILevel[12, atomicservice : true]
public class StructTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): StructTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): StructTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): StructTypeInfo
    
    /**
    * Returns the collection of public constructors of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop constructors: Collection<ConstructorInfo>
    
    /**
    * Returns the collection of public instance variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop instanceVariables: Collection<InstanceVariableInfo>
    
    /**
    * Returns the collection of public static variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop staticVariables: Collection<StaticVariableInfo>
    
    /**
    * Creates a new instance struct with corresponding type for incoming args.
    *
    * @param args parameter list.
    *
    * @throw MisMatchException, if no public constructor is found to construct new instance.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called to construct the instance.
    */
    @!APILevel[12, atomicservice : true]
    public func construct(args: Array<Any>): Any
    
    /**
    * Searches the type info's public constructor by incoming parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getConstructor(parameterTypes: Array<TypeInfo>): ConstructorInfo
    
    /**
    * Searches the type info's public instance variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceVariable(name: String): InstanceVariableInfo
    
    /**
    * Searches the type info's public static variable by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticVariable(name: String): StaticVariableInfo
}

/**
* Contains reflective information about structs.
*/
@!APILevel[12, atomicservice : true]
public open class StructTypeInfo <: TypeInfo {
    @!APILevel[12, atomicservice : true]
    public redef static func get(qualifiedName: String): StructTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of(a: Any): StructTypeInfo
    
    @!APILevel[12, atomicservice : true]
    public redef static func of<T>(): StructTypeInfo
    
    /**
    * Returns the collection of public constructors of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop constructors: Collection<ConstructorInfo>
    
    /**
    * Returns the collection of public instance variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop instanceVariables: Collection<InstanceVariableInfo>
    
    /**
    * Returns the collection of public static variables of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop staticVariables: Collection<StaticVariableInfo>
    
    /**
    * Creates a new instance struct with corresponding type for incoming args.
    *
    * @param args parameter list.
    *
    * @throw MisMatchException, if no public constructor is found to construct new instance.
    * @throw InvocationTargetException, if an exception is thrown when the constructor is called to construct the instance.
    */
    @!APILevel[12, atomicservice : true]
    public func construct(args: Array<Any>): Any
    
    /**
    * Searches the type info's public constructor by incoming parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getConstructor(parameterTypes: Array<TypeInfo>): ConstructorInfo
    
    /**
    * Searches the type info's public instance variable by incoming name.
    *
    * @throw IllegalArgumentException, if name is empty or is an invalid identifier string.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceVariable(name: String): InstanceVariableInfo
    
    /**
    * Searches the type info's public static variable by incoming name.
    *
    * @throw IllegalArgumentException, if name is empty or is an invalid identifier string.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticVariable(name: String): StaticVariableInfo
}

// TODO remove when String will be supported on BE
@!APILevel[12, atomicservice : true]
public class StringTypeInfo <: StructTypeInfo {
    @!APILevel[12, atomicservice : true]
    public override prop name: String
    
    @!APILevel[12, atomicservice : true]
    public override prop instanceFunctions: Collection<InstanceFunctionInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop staticFunctions: Collection<StaticFunctionInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop instanceProperties: Collection<InstancePropertyInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop staticProperties: Collection<StaticPropertyInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop annotations: Collection<Annotation>
    
    @!APILevel[12, atomicservice : true]
    public override prop superInterfaces: Collection<InterfaceTypeInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop modifiers: Collection<ModifierInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop constructors: Collection<ConstructorInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop instanceVariables: Collection<InstanceVariableInfo>
    
    @!APILevel[12, atomicservice : true]
    public override prop staticVariables: Collection<StaticVariableInfo>
}

/**
* This file defines the `toAny` methods for each primitive type.
*/
extend Object {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Int64 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend IntNative {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Int32 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Int16 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Int8 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend UInt64 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend UIntNative {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend UInt32 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend UInt16 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend UInt8 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Float64 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Float32 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Float16 {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Bool {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Rune {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend Unit {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

extend<Object> Array<Object> {
    @!APILevel[12, atomicservice : true]
    public func toAny(): Any
}

/**
* Contains the reflective information that is common for classes, interfaces, structs and primitive types.
*/
@!APILevel[12, atomicservice : true]
sealed abstract class TypeInfo <: Equatable<TypeInfo> & Hashable & ToString {
    /**
    * Creates the certain type info by generic type T.
    */
    @!APILevel[12, atomicservice : true]
    public static func of<T>(): TypeInfo
    
    /**
    * Creates the certain type info for incoming instance with 'Any' type.
    */
    @!APILevel[12, atomicservice : true]
    public static func of(a: Any): TypeInfo
    
    /**
    * Creates the class type info for incoming instance with 'Object' type.
    */
    @Deprecated[message:"Use 'ClassTypeInfo.get(Object)' instead."]
    @!APILevel[12, atomicservice : true]
    public static func of(a: Object): ClassTypeInfo
    
    /**
    * Searches and creates the certain type info by incoming qualified name.
    * If no corresponding type information for incoming qualified name is found, the exception is thrown.
    */
    @!APILevel[12, atomicservice : true]
    public static func get(qualifiedName: String): TypeInfo
    
    /**
    * Returns the simple name of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the qualified name of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop qualifiedName: String
    
    /**
    * Returns the collection of public instance functions of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop instanceFunctions: Collection<InstanceFunctionInfo>
    
    /**
    * Returns the list of public static functions of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop staticFunctions: Collection<StaticFunctionInfo>
    
    /**
    * Returns the collection of public instance properties of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop instanceProperties: Collection<InstancePropertyInfo>
    
    /**
    * Returns the collection of public static properties of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop staticProperties: Collection<StaticPropertyInfo>
    
    /**
    * Returns the collection of annotations of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns the collection of super interfaces of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop superInterfaces: Collection<InterfaceTypeInfo>
    
    /**
    * Returns the collection of modifiers of type info.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns true, if current type info is subtype of incoming type info, false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isSubtypeOf(supertype: TypeInfo): Bool
    
    /**
    * Searches the type info's public instance function by incoming name and parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceFunction(name: String, parameterTypes: Array<TypeInfo>): InstanceFunctionInfo
    
    @!APILevel[12, atomicservice : true]
    public func getInstanceFunctions(name: String): Array<InstanceFunctionInfo>
    
    /**
    * Searches the type info's public static function by incoming name and parameter types.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticFunction(name: String, parameterTypes: Array<TypeInfo>): StaticFunctionInfo
    
    @!APILevel[12, atomicservice : true]
    public func getStaticFunctions(name: String): Array<StaticFunctionInfo>
    
    /**
    * Searches the type info's public instance property by incoming name and signature.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceProperty(name: String): InstancePropertyInfo
    
    /**
    * Searches the type info's public static property by incoming name and signature.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticProperty(name: String): StaticPropertyInfo
    
    /**
    * Searches the type info's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T>
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: TypeInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: TypeInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information that is common for classes, interfaces, structs and primitive types.
*/
@!APILevel[12, atomicservice : true]
sealed abstract class TypeInfo <: Equatable<TypeInfo> & Hashable & ToString {
    /**
    * Creates the certain type info by generic type T.
    */
    @!APILevel[12, atomicservice : true]
    public static func of<T>(): TypeInfo
    
    /**
    * Creates the certain type info for incoming instance with 'Any' type.
    *
    * @param a instance of Any type.
    */
    @!APILevel[12, atomicservice : true]
    public static func of(a: Any): TypeInfo
    
    /**
    * Creates the class type info for incoming instance with 'Object' type.
    *
    * @param a instance of Object type.
    */
    @Deprecated[message:"Use 'ClassTypeInfo.get(Object)' instead."]
    @!APILevel[12, atomicservice : true]
    public static func of(a: Object): ClassTypeInfo
    
    /**
    * Searches and creates the certain type info by incoming qualified name.
    *
    * @param qualifiedName qualified name of the type.
    *
    * @throws MalformedQualifiedNameException, if no corresponding type information for incoming qualified name is found.
    * @throws InfoNotFoundException, if the type can not be found.
    */
    @!APILevel[12, atomicservice : true]
    public static func get(qualifiedName: String): TypeInfo
    
    /**
    * Returns the simple name of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop name: String
    
    /**
    * Returns the qualified name of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop qualifiedName: String
    
    /**
    * Returns the collection of public instance functions of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop instanceFunctions: Collection<InstanceFunctionInfo>
    
    /**
    * Returns the list of public static functions of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop staticFunctions: Collection<StaticFunctionInfo>
    
    /**
    * Returns the collection of public instance properties of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop instanceProperties: Collection<InstancePropertyInfo>
    
    /**
    * Returns the collection of public static properties of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop staticProperties: Collection<StaticPropertyInfo>
    
    /**
    * Returns the collection of annotations of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop annotations: Collection<Annotation>
    
    /**
    * Returns the collection of super interfaces of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop superInterfaces: Collection<InterfaceTypeInfo>
    
    /**
    * Returns the collection of modifiers of type info.
    */
    @!APILevel[12, atomicservice : true]
    public open prop modifiers: Collection<ModifierInfo>
    
    /**
    * Determines whether the type is a subtype of the specified type.
    *
    * @param supertype, Specifies the parent type typeInfo.
    * @return Bool Returns true, if current type info is subtype of incoming type info, false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isSubtypeOf(supertype: TypeInfo): Bool
    
    /**
    * Searches the type info's public instance function by incoming name and parameter types.
    *
    * @param name function name.
    * @param parameterTypes parameter list of the function.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceFunction(name: String, parameterTypes: Array<TypeInfo>): InstanceFunctionInfo
    
    /**
    * Searches the type info's public static function by incoming name and parameter types.
    *
    * @param name function name.
    * @param parameterTypes parameter list of the function.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticFunction(name: String, parameterTypes: Array<TypeInfo>): StaticFunctionInfo
    
    /**
    * Searches the type info's public instance property by incoming name and signature.
    *
    * @param name function name.
    */
    @!APILevel[12, atomicservice : true]
    public func getInstanceProperty(name: String): InstancePropertyInfo
    
    /**
    * Searches the type info's public static property by incoming name and signature.
    *
    * @param name property name.
    */
    @!APILevel[12, atomicservice : true]
    public func getStaticProperty(name: String): StaticPropertyInfo
    
    /**
    * Searches the type info's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: TypeInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: TypeInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

public type Annotation = Object

/**
* Parses the input string as a TypeInfo array
*
* @param parameterTypes the parameter type part of the function type.
* It does not contain the parameter name, default value, or outermost parenthesis.
* @return TypeInfo array converted from a parameter type string.
*
* @throws IllegalArgumentException if the input string does not comply with the specifications
*/
@!APILevel[12, atomicservice : true]
public func parseParameterTypes(parameters: String): Array<TypeInfo>

/**
* Contains the reflective information about instance variables.
*/
@!APILevel[12, atomicservice : true]
public class InstanceVariableInfo <: Equatable<InstanceVariableInfo> & Hashable & ToString {
    /**
    * Returns the name of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance variable is 'mut' and typeInfo is not struct
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the instance variable's value for incoming instance.
    *
    * @throw IllegalTypeException, if the input instance type is different from the instance which this InstanceVariableInfo belong to.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(instance: Any): Any
    
    /**
    * Updates the instance variable's value with incoming new value for incoming instance.
    *
    * @param instance which the member variable belongs.
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the instance variable is immutable.
    * @throw IllegalTypeException, if the input newValue type is different from the instance variable type.
    * @throw UnsupportedException, if this variable is struct
    * @throw IllegalTypeException, if the input instance type is different from the instance which this InstanceVariableInfo belong to.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(instance: Any, newValue: Any): Unit
    
    /**
    * Searches the instance variable's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstanceVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstanceVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static variables.
*/
@!APILevel[12, atomicservice : true]
public class StaticVariableInfo <: Equatable<StaticVariableInfo> & Hashable & ToString {
    /**
    * Returns the name of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if static variable is 'mut' and typeInfo is not struct
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the static variable's value.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the static variable's value with incoming new value.
    *
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the static variable is immutable.
    * @throw UnsupportedException, if this variable is struct
    * @throw IllegalTypeException, if the input newValue type is different from the static variable type.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    /**
    * Searches the static variable's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about global variables.
*/
@!APILevel[12, atomicservice : true]
public class GlobalVariableInfo <: Equatable<GlobalVariableInfo> & Hashable & ToString {
    /**
    * Returns the name of global variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    // variable type
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    // Annotation, when there is no data, size is 0. Note that this api does not guarantee a constant traversal order.
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    // Whether it is a variable modified by var and typeInfo is not struct. Calling setValue on a variable modified by let will throw an exception.
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    // get variable value
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the global variable's value with incoming new value.
    *
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the global variable is immutable.
    * @throw IllegalTypeException, if the input newValue type is different from the global variable type.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    /**
    * Searches the global variable's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: GlobalVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: GlobalVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about instance variables.
*/
@!APILevel[12, atomicservice : true]
public class InstanceVariableInfo <: Equatable<InstanceVariableInfo> & Hashable & ToString {
    /**
    * Returns the name of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of instance variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if instance variable is 'mut', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the instance variable's value for incoming instance.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(instance: Any): Any
    
    /**
    * Updates the instance variable's value with incoming new value for incoming instance.
    *
    * @param instance which the member variable belongs.
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the instance variable is immutable.
    * @throw IllegalTypeException, if the input newValue type is different from the instance variable type.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(instance: Any, newValue: Any): Unit
    
    /**
    * Searches the instance variable's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: InstanceVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: InstanceVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about static variables.
*/
@!APILevel[12, atomicservice : true]
public class StaticVariableInfo <: Equatable<StaticVariableInfo> & Hashable & ToString {
    /**
    * Returns the name of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    /**
    * Returns the return type of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    /**
    * Returns the collection of modifiers of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop modifiers: Collection<ModifierInfo>
    
    /**
    * Returns the collection of annotations of static variable.
    */
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    /**
    * Returns true if static variable is 'mut', false otherwise.
    */
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    /**
    * Returns the static variable's value.
    */
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the static variable's value with incoming new value.
    *
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the static variable is immutable.
    * @throw IllegalTypeException, if the input newValue type is different from the static variable type.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    /**
    * Searches the static variable's annotation by incoming name.
    */
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: StaticVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: StaticVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

/**
* Contains the reflective information about global variables.
*/
@!APILevel[12, atomicservice : true]
public class GlobalVariableInfo <: Equatable<GlobalVariableInfo> & Hashable & ToString {
    // variable name
    @!APILevel[12, atomicservice : true]
    public prop name: String
    
    // variable type
    @!APILevel[12, atomicservice : true]
    public prop typeInfo: TypeInfo
    
    // Annotation, when there is no data, size is 0. Note that this api does not guarantee a constant traversal order.
    @!APILevel[12, atomicservice : true]
    public prop annotations: Collection<Annotation>
    
    // Whether it is a variable modified by var. Calling setValue on a variable modified by let will throw an exception.
    @!APILevel[12, atomicservice : true]
    public func isMutable(): Bool
    
    // get variable value
    @!APILevel[12, atomicservice : true]
    public func getValue(): Any
    
    /**
    * Updates the global variable's value with incoming new value.
    *
    * @param newValue new value to set.
    *
    * @throw IllegalSetException, if the global variable is immutable.
    * @throw IllegalTypeException, if the input newValue type is different from the global variable type.
    */
    @!APILevel[12, atomicservice : true]
    public func setValue(newValue: Any): Unit
    
    // Get the annotation by name, return None if not found
    @!APILevel[12, atomicservice : true]
    public func findAnnotation<T>(): Option<T> where T <: Annotation
    
    @!APILevel[12, atomicservice : true]
    public operator func ==(that: GlobalVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public operator func !=(that: GlobalVariableInfo): Bool
    
    @!APILevel[12, atomicservice : true]
    public func hashCode(): Int64
    
    @!APILevel[12, atomicservice : true]
    public func toString(): String
}

