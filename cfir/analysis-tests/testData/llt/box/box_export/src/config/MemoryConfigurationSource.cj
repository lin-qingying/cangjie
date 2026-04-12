// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.collection.HashMap

/*
内存配置源
 */
class MemoryConfigurationSource <: ConfigurationSource {
    private let _items: HashMap<String, String>

    init(items: HashMap<String, String>) {
        _items = items
    }

    public func build() {
        _items
    }
}
