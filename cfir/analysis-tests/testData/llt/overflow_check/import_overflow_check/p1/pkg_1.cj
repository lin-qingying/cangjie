// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

public func fun_1(para_1: Int32, para_2: Int32, para_3!: Int32 = -10) {
    var pri_v1: Int32 = para_1
    var pri_v2: Int32 = para_2
    var pri_v3: Int32 = para_3
    return pri_v1 + pri_v2 + para_3
}
