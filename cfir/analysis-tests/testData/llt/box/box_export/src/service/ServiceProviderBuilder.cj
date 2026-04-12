// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
