// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pb

struct S1 <: I1 {}

struct S2 <: I2 {}

let i = if (true) {
    S1()
} else {
    S2()
}
