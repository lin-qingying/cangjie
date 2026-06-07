// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main():Int64 {
    var i2: Int64 = 0
    print("indexOf('?', 5)=" <!parse_invalid_left_hand_expr!>+=<!> Int32(i2).toString() + "\n")
    return 0
}
