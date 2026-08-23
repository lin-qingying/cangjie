// FILE: ServiceLifetime.cj


package asp.service

/*
服务声明周期
 */
public enum ServiceLifetime {
    Singleton | Scoped | Transient

    public func isScoped() {
        match (this) {
            case Scoped => true
            case _ => false
        }
    }

    public func isTransient() {
        match (this) {
            case Transient => true
            case _ => false
        }
    }

    public func isSingleton() {
        match (this) {
            case Singleton => true
            case _ => false
        }
    }
}
// FILE: ServiceDescriptor.cj


package asp.service

import std.reflect.TypeInfo

/*
服务描述信息
 */
public class ServiceDescriptor {
    private let _serviceType: TypeInfo
    private let _implementType: TypeInfo
    private let _factory: ?Any
    private let _lifetime: ServiceLifetime

    init(serviceType: TypeInfo, implementType: TypeInfo, lifetime: ServiceLifetime) {
        _serviceType = serviceType
        _implementType = implementType
        _factory = None
        _lifetime = lifetime
    }

    init(serviceType: TypeInfo, implementType: TypeInfo, factory: Object, lifetime: ServiceLifetime) {
        _serviceType = serviceType
        _implementType = implementType
        _factory = factory
        _lifetime = lifetime
    }

    init(serviceType: TypeInfo, implementType: TypeInfo, factory: (ServiceProvider) -> Object, lifetime: ServiceLifetime) {
        _serviceType = serviceType
        _implementType = implementType
        _factory = factory
        _lifetime = lifetime
    }

    public prop serviceType: TypeInfo {
        get() {
            _serviceType
        }
    }

    public prop implementType: TypeInfo {
        get() {
            _implementType
        }
    }

    public prop lifetime: ServiceLifetime {
        get() {
            _lifetime
        }
    }

    public prop factory: ?Any {
        get() {
            _factory
        }
    }
}
// FILE: ServiceProvider.cj


package asp.service

import std.reflect.TypeInfo
import std.collection.{ArrayList, HashMap, last, collectArray}

/*
服务提供者
 */
public class ServiceProvider <: Resource {
    private let _isRoot: Bool
    private var _isClosed = false
    private let _root: ?ServiceProvider
    private let _services = HashMap<TypeInfo, Object>()
    private let _descriptors: HashMap<TypeInfo, ArrayList<ServiceDescriptor>>

    init(descriptors: HashMap<TypeInfo, ArrayList<ServiceDescriptor>>) {
        _root = None
        _isRoot = true
        _descriptors = descriptors
    }

    private init(root: ServiceProvider, descriptors: HashMap<TypeInfo, ArrayList<ServiceDescriptor>>) {
        _root = root
        _isRoot = false
        _descriptors = descriptors
    }

    public func getService(typeInfo: TypeInfo): ?Object {
        return createService(typeInfo)
    }

    public func getService<T>(): ?T {
        return createService(TypeInfo.of<T>()) as T
    }

    public func getRequiredService<T>(): T {
        let typeInfo = TypeInfo.of<T>()
        if (!_descriptors.contains(typeInfo)) {
            throw Exception("Service of type '${typeInfo}' is not registered.")
        }
        return getService<T>().getOrThrow()
    }

    public func createScope(): ServiceProvider {
        return ServiceProvider(this, _descriptors)
    }

    private func createService(typeInfo: TypeInfo): Object {
        if (typeInfo.isSubtypeOf(TypeInfo.of<ServiceProvider>())) {
            return this
        }
        let descriptorOption = (_descriptors[typeInfo] |> last)
        if (descriptorOption.isNone()) {
            throw Exception("Service of type '${typeInfo}' is not registered.")
        }

        let descriptor = descriptorOption.getOrThrow()

        if (!descriptor.lifetime.isSingleton() && _isRoot) {
            throw Exception("Cannot resolve non-singleton service from the root container.")
        }

        if (descriptor.lifetime.isSingleton() && let Some(root) <- _root) {
            return root.createService(typeInfo)
        }

        if (!descriptor.lifetime.isTransient() && _services.contains(typeInfo)) {
            return _services[typeInfo]
        }

        let instance = createService(descriptor)

        if (!descriptor.lifetime.isTransient()) {
            _services[descriptor.serviceType] = instance
        }

        return instance
    }

    private func createService(descriptor: ServiceDescriptor): Object {
        if (let Some(factory) <- descriptor.factory) {
            if (let Some(apply) <- (factory as (ServiceProvider) -> Object)) {
                return apply(this)
            } else if (let Some(instance) <- (factory as Object)) {
                return instance
            } else {
                throw Exception("Service of type '${descriptor.serviceType}' is not registered.")
            }
        } else {
            let constructor = ReflectUtilities.getServiceConstructor(descriptor.implementType)
            let args = ArrayList<Any>()
            for (pattern in constructor.parameters) {
                let arg = createService(pattern.typeInfo)
                args.add(arg)
            }
            return (constructor.apply(args |> collectArray) as Object).getOrThrow()
        }
    }

    public func close() {
        if (!isClosed()) {
            for (pattern in _services.values()) {
                if (let Some(reource) <- (pattern as Resource)) {
                    reource.close()
                }
            }
        }
        _isClosed = true
    }

    public func isClosed() {
        _isClosed
    }
}
// FILE: ServiceProviderBuilder.cj


package asp.service

import std.reflect.TypeInfo
import std.collection.{ArrayList, HashMap}

/*
服务提供者构建器
 */
public class ServiceProviderBuilder {
    let _descriptors = ArrayList<ServiceDescriptor>()

    public func addSingleton<TService>(instance: TService) {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of(instance)
        append(ServiceDescriptor(serviceType, implementType, (instance as Object).getOrThrow(),
            ServiceLifetime.Singleton))
        return this
    }

    public func addSingleton(serviceType: TypeInfo, implementType: TypeInfo) {
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Singleton))
        return this
    }

    public func addScoped(serviceType: TypeInfo, implementType: TypeInfo) {
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Scoped))
        return this
    }

    public func addTransient(serviceType: TypeInfo, implementType: TypeInfo) {
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Transient))
        return this
    }

    public func addSingleton<TService, TImplement>() {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Singleton))
        return this
    }

    public func addScoped<TService, TImplement>() {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Scoped))
        return this
    }

    public func addTransient<TService, TImplement>() {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, ServiceLifetime.Transient))
        return this
    }

    public func addSingleton<TService, TImplement>(factory: (ServiceProvider) -> TImplement) where TImplement <: Object {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, factory, ServiceLifetime.Singleton))
        return this
    }

    public func addScoped<TService, TImplement>(factory: (ServiceProvider) -> TImplement) where TImplement <: Object {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, factory, ServiceLifetime.Scoped))
        return this
    }

    public func addTransient<TService, TImplement>(factory: (ServiceProvider) -> TImplement) where TImplement <: Object {
        let serviceType = TypeInfo.of<TService>()
        let implementType = TypeInfo.of<TImplement>()
        append(ServiceDescriptor(serviceType, implementType, factory, ServiceLifetime.Transient))
        return this
    }

    public func append(descriptor: ServiceDescriptor) {
        if (!descriptor.implementType.isSubtypeOf(descriptor.serviceType)) {
            throw Exception(
                "Implement type '${descriptor.implementType}' is not a subtype of service type '${descriptor.serviceType}'")
        }
        _descriptors.add(descriptor)
    }

    public func build() {
        let descriptor = HashMap<TypeInfo, ArrayList<ServiceDescriptor>>()
        for (pattern in _descriptors) {
            if (!descriptor.contains(pattern.serviceType)) {
                descriptor[pattern.serviceType] = ArrayList<ServiceDescriptor>()
            }
            descriptor[pattern.serviceType].add(pattern)
        }
        return ServiceProvider(descriptor)
    }
}
// FILE: ReflectUtilities.cj


package asp.service

import std.reflect.{TypeInfo, ClassTypeInfo}
import std.collection.{collectArrayList, first}

/*
反射工具类
 */
class ReflectUtilities {
    public static func getServiceConstructor(typeInfo: TypeInfo) {
        if (let Some(classInfo) <- (typeInfo as ClassTypeInfo)) {
            let list = classInfo.constructors |> collectArrayList
            list.sortBy {
                a, b => if (a.parameters.size > b.parameters.size) {
                    Ordering.LT
                } else if (a.parameters.size < b.parameters.size) {
                    Ordering.GT
                } else {
                    Ordering.EQ
                }
            }
            return (list |> first).getOrThrow {Exception("No constructor found")}
        }
        throw Exception("The '${typeInfo}' not class type.")
    }
}
// FILE: ActivatorUtilties.cj


package asp.service

import std.reflect.{<!UNUSED_IMPORT!>ClassTypeInfo<!>, TypeInfo}
import std.collection.{ArrayList, first, filter, collectArray, <!UNUSED_IMPORT!>collectArrayList<!>}

/*
用于基于容器提供者构建未注册单依赖容器服务的实列
 */
public class ActivatorUtilties {
    public static func createInstance<T>(sp: ServiceProvider, args: Array<Any>) {
        let constructor = ReflectUtilities.getServiceConstructor(TypeInfo.of<T>())
        let parameters = ArrayList<Any>()
        for (pattern in constructor.parameters) {
            let parameter = sp.getService(pattern.typeInfo)
            if (parameter.isNone()) {
                let arg = args |> filter {a => pattern.typeInfo.isSubtypeOf(TypeInfo.of(a))} |> first
                parameters.add(arg.getOrThrow())
            } else {
                parameters.add(parameter.getOrThrow())
            }
        }
        let obj = constructor.apply(parameters |> collectArray) as T
        return obj.getOrThrow()
    }
}
