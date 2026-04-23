// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int8 {
    var a: Int8 = 0;
    match {
        case _ => a
    } >> (match {
        case _ => (1, a)[1]
    } & 7)
}
