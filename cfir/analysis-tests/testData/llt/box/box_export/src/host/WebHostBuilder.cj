// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import asp.service.ServiceProviderBuilder
import asp.config.ConfigurationManager

public class WebHostBuilder {
    private let _services: ServiceProviderBuilder
    private let _environment: HostEnvironment
    private let _configuration: ConfigurationManager

    protected init(services: ServiceProviderBuilder, environment: HostEnvironment, configuration: ConfigurationManager) {
        _services = services
        _environment = environment
        _configuration = configuration
    }

    public prop services: ServiceProviderBuilder {
        get() {
            _services
        }
    }

    public prop environment: HostEnvironment {
        get() {
            _environment
        }
    }

    public prop configuration: ConfigurationManager {
        get() {
            _configuration
        }
    }

    public func build() {
        let service = _services.build()
        let configuration = _configuration.build()
        WebHost(service, _environment, configuration)
    }
}
