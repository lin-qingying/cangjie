// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

import std.collection.HashMap

/*
配置源
 */
public abstract class ConfigurationSource {
    public func build(): HashMap<String, String>
}
