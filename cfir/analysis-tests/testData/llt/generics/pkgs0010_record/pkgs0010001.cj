// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkgs0010001a.*

main(): Int64 {
    var value = Test<Int32>(1)
    var b = value.member1 + 10

    if (value.member1 == 1 && b == 11) {
        return 0
    } else {
        return 1
    }
}
