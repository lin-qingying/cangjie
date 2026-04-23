// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

let t = while (true) {
    var l: Int32 = 0
    var r: Int64 = 1
    l <!INVALID_BINARY_OPERATOR!>+<!> r
}
