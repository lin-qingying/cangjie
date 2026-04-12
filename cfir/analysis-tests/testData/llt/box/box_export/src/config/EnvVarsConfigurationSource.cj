// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.process.Process
import std.collection.HashMap

/*
环境变量配置源
 */
class EnvVarsConfigurationSource <: ConfigurationSource {
    let _prefix: String

    init(prefix: String) {
        _prefix = prefix.toAsciiUpper()
    }

    public func build() {
        let items = HashMap<String, String>()
        let envVars = Process.current.environment
        for ((key, value) in envVars) {
            if (!key.toAsciiUpper().startsWith(_prefix)) {
                continue
            }
            let name = key.toAsciiUpper().removePrefix("${_prefix}_").toAsciiLower()
            items.add(name, value)
        }
        return items
    }
}
