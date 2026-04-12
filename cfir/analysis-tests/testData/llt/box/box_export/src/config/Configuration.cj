// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package asp.config

/*
配置核心接口
 */
public interface Configuration <: Iterable<(String, String)> {
    operator func [](name: String): String
    func get(name: String): Option<String>
    func getConnectionString(name: String): ?String
}
