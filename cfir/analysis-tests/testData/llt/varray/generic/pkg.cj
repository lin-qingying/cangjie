// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkg

public func A<T>(): Int64 {
    var v: VArray<Int64, $1> = [1]
    let b = VArray<Int64, $5>({i => i})
    return v[0] + b[1]
}
