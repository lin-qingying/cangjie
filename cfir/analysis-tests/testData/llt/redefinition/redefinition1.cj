// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func doo() {}

func <!CONFLICTING_OVERLOADS!>doo<!>(i: Int64): Int32 {
    return 1
}

func doo(i: Int64): Int32 {
    return 1
}

func doo(i: Int32) {}

func doo(i: Float32) {}
