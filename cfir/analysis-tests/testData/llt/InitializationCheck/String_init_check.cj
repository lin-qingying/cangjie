// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var str1: String
    var str2 = <!USED_BEFORE_INITIALIZATION!>str1<!> + "4"
    print(str2)
    return 0
}
