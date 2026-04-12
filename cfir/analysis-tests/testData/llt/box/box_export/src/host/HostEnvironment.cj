// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.host

import std.process.Process
import asp.config.Configuration

/*
Host环境
 */
public class HostEnvironment {
    private let _configuration: Configuration

    protected init(configuration: Configuration) {
        _configuration = configuration
    }

    /*
    环境名称
     */
    public prop name: String {
        get() {
            return _configuration.get("environment") ?? "prod"
        }
    }

    /*
    是否生产环境
     */
    public func isProd() {
        name == "prod"
    }

    /*
    是否开发环境
     */
    public func isDev() {
        name == "dev"
    }

    /*
    是否测试环境
     */
    public func isTest() {
        name == "test"
    }

    public func isEnvironment(name: String) {
        this.name == name
    }

    public prop contentRootPath: String {
        get() {
            _configuration.get("contentRootPath") ?? Process.current.workingDirectory.toString()
        }
    }
}
