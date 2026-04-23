// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main():Int64 {
    let a = 10
    var b = match (a) {
        case 11 => 1
        case c@_ => 2
    }
    return 0
}
