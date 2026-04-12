// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.collection.HashMap
import std.io.{IOStream}

/*
配置构建接口
 */
public interface ConfigurationBuilder {
    func add(source: ConfigurationSource): ConfigurationBuilder
    func addMemory(items: HashMap<String, String>): ConfigurationBuilder
    func addArgOpts(args: Array<String>): ConfigurationBuilder
    func addEnvVars(prefix: String): ConfigurationBuilder
    func addJsonStream(stream: IOStream): ConfigurationBuilder
    func addJsonFile(path: String, optional: Bool): ConfigurationBuilder
    func build(): Configuration
}
