// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt  %f  -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
// ASSERT: regex-not Segementation

main(): Int64 {
    var a = "\r !'#$%&()*+,}~\f-./012345678\v9:;<=>?@ABCDEFGHIJKLM\nNOPQRSTUVWXYZ[]^_`\tabcdefghi\bjklmn\0opqrs\\tu\''vwxyz{|\""
        .toArray()
    var b: Array<UInt8> = [13, 32, 33, 39, 35, 36, 37, 38, 40, 41, 42, 43, 44, 125, 126, 12, 45, 46, 47, 48, 49, 50, 51,
        52, 53, 54, 55, 56, 11, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 10,
        78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 93, 94, 95, 96, 9, 97, 98, 99, 100, 101, 102, 103, 104,
        105, 8, 106, 107, 108, 109, 110, 0, 111, 112, 113, 114, 115, 92, 116, 117, 39, 39, 118, 119, 120, 121, 122, 123,
        124, 34]
    for (i in 0..a.size) {
        if (a[i] != b[i]) {
            return 1
        }
        if (!(a[i] is UInt8)) {
            return 2
        }
    }
    return 0
}
