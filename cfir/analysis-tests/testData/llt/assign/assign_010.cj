// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let myLetUInt8: UInt8 = 0
    if (true) {
        print("${myLetUInt8}")
    } else {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>myLetUInt8<!> = 12
    }
}
