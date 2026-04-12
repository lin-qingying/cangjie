// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var a1 = UInt8(UInt32(r'\n'))
    if (a1 != 10) {
        print("${a1}")
        return 1
    }
    var a2 = UInt8(UInt32(r'\0'))
    if (a2 != 0) {
        print("a2\n")
        return 1
    }
    var a3 = UInt8(UInt32(r'\r'))
    if (a3 != 13) {
        print("a3\n")
        return 1
    }
    var a4 = UInt8(UInt32(r'\v'))
    if (a4 != 11) {
        print("a4\n")
        return 1
    }
    var a5 = UInt8(UInt32(r'\f'))
    if (a5 != 12) {
        print("a5\n")
        return 1
    }
    var a6 = UInt8(UInt32(r'\"'))
    if (a6 != 34) {
        print("a6\n")
        return 1
    }
    var a7 = UInt8(UInt32(r'\''))
    if (a7 != 39) {
        print("a7 : ${a7}\n")
        return 1
    }
    var a8 = UInt8(UInt32(r'\t'))
    if (a8 != 9) {
        print("a8\n")
        return 1
    }
    var s1 = UInt8(UInt32(r'x'))
    if (s1 != 120) {
        print("s1\n")
        return 1
    }
    var s2 = UInt8(UInt32(r'-'))
    if (s2 != 45) {
        print("s2\n")
        return 1
    }
    var s3 = UInt8(UInt32(r'~'))
    if (s3 != 126) {
        print("s3\n")
        return 1
    }
    var s4 = UInt8(UInt32(r'\\'))
    if (s4 != 92) {
        print("s4\n")
        return 1
    }
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
