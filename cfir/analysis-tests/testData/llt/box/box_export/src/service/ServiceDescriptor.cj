

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
