// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.collection.HashMap
import std.io.{InputStream, StringReader, IOStream}
import std.fs.{exists, File, OpenMode}

/*
配置管理器
用于构建配置和读取配置
 */
public class ConfigurationManager <: Configuration & ConfigurationBuilder {
    let _items = HashMap<String, String>()

    public init() {
    }

    public init(builder: (ConfigurationBuilder) -> Unit) {
        builder(this)
    }

    public func iterator() {
        return _items.iterator()
    }

    public operator func [](name: String) {
        return _items[name]
    }

    public func get(name: String) {
        return _items.get(name)
    }

    public func getConnectionString(name: String) {
        return get("connectionStrings:${name}")
    }

    public func add(source: ConfigurationSource): ConfigurationBuilder {
        let items = source.build()
        _items.add(all: items)
        return this
    }

    public func addMemory(items: HashMap<String, String>): ConfigurationBuilder {
        add(MemoryConfigurationSource(items))
        return this
    }

    public func addArgOpts(args: Array<String>): ConfigurationBuilder {
        add(ArgOptsConfigurationSource(args))
        return this
    }

    public func addEnvVars(prefix: String): ConfigurationBuilder {
        add(EnvVarsConfigurationSource(prefix))
        return this
    }

    public func addJsonStream(stream: IOStream): ConfigurationBuilder {
        let sr = StringReader(stream)
        let json = sr.readToEnd()
        add(JosnConfigurationSource(json))
        return this
    }

    public func addJsonFile(path: String, optional: Bool): ConfigurationBuilder {
        if (optional && !exists(path)) {
            return this
        }
        try (file = File(path, OpenMode.Read)) {
            addJsonStream(file)
        }
        return this
    }

    public func build(): Configuration {
        return this
    }
}
