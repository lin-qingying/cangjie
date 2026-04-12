// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

internal import std.collection.*

func IncBuf<T>(data: ArrayList<T>) {
    data[0] += 1
    return 0
}

main() {
    var gStateBufI8 = ArrayList<Int8>([-1, 1])
    IncBuf<Int8>(gStateBufI8)
    return 0
}

// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

// ASSERT: regex-auto :6:.* type Class-ArrayList<Generics-T> does not have both operator func [](index) and operator func [](index, value) for index type Int64
// ASSERT: regex-auto :6:.* type incompatible in this compound assignment expression
