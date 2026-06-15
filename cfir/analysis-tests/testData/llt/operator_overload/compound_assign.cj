// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import std.collection.*

func IncBuf<T>(data: ArrayList<T>) {
    data[0] <!INVALID_BINARY_OPERATOR!>+<!>= 1
    return 0
}

main() {
    var gStateBufI8 = ArrayList<Int8>([-1, 1])
    IncBuf<Int8>(gStateBufI8)
    return 0
}
