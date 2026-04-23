// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ser.cj ser2.cj

package test

import std.unittest.*
import std.collection.*

main(): Int64 {
    var l: ArrayList<String> = ArrayList<String>(["a", "b", "c"])
    var seris: DataModel = l.serialize()
    ArrayList<String>.deserialize(seris)
    return 0
}
