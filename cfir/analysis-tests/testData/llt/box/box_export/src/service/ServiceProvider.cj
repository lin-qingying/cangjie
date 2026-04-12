// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
