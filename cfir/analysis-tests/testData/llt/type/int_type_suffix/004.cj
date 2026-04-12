// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func unhex(c: Byte): Byte {
    match {
        case UInt8(UInt32(r'0')) <= c && c <= UInt8(UInt32(r'9')) => c - UInt8(UInt32(r'0'))
        case UInt8(UInt32(r'a')) <= c && c <= UInt8(UInt32(r'f')) => c - UInt8(UInt32(r'a')) + 10
        case UInt8(UInt32(r'A')) <= c && c <= UInt8(UInt32(r'F')) => c - UInt8(UInt32(r'A')) + 10
        case _ => 0
    }
}

main() {
    let char: UInt8 = 97
    var res = match (char) {
        case 'a' => 0
        case _ => 1
    }
    if (res != 0) {
        return 1
    }
    let a = "\u{2}\u{10}".toArray()
    let sum = a[0] + a[1]
    if (sum != 18) {
        return 1
    }
    let b = UInt8(UInt32(r'a')) + UInt8(UInt32(r'\n')) + 3u8 + 4_u8
    if (b != 114) {
        return 1
    }
    let c = UInt8(UInt32(r'a')) - UInt8(UInt32(r'A'))
    if (c != 0x20) {
        return 1
    }
    let h1 = unhex(UInt8(UInt32(r'7')))
    if (h1 != 7) {
        return 1
    }
    let h2 = unhex(UInt8(UInt32(r'e')))
    if (h2 != 14) {
        return 1
    }
    let h3 = unhex(UInt8(UInt32(r'C')))
    if (h3 != 12) {
        return 1
    }
    return 0
}
