// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.collection.HashMap

/*
命令行参数配置源
 */
class ArgOptsConfigurationSource <: ConfigurationSource {
    let _args: Array<String>

    init(args: Array<String>) {
        _args = args
    }

    public func build() {
        let items = HashMap<String, String>()
        for (item in _args) {
            let keyValue = item.split('=', 2)
            if (keyValue.size < 2) {
                continue
            }
            items.add(keyValue[0].trimEnd().trimStart(), keyValue[1].trimEnd().trimStart())
        }
        return items
    }
}
