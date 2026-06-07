// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var tuple = (1, 2, 3)
    <!UNQUALIFIED_LEFT_VALUE_ASSIGNED!>tuple[0]<!>++
    return tuple[0]
}
