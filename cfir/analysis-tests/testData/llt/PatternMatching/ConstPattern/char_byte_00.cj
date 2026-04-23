// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main() {
    let x = UInt8(UInt32(r'A'))
    match (x) {
        case 'A' | 'B' => 0
        case _ => 1
    }
}
