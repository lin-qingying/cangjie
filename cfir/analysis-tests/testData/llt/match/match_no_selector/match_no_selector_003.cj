// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var b: Int64 = match {
        case true => 0
        case <!TYPE_MISMATCH!>1<!> => 2
        case _ => 3
    }
    return b
}
