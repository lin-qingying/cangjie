// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.collection.ArrayList

main(): Int64 {
    var a1 = "abcdefg"
    var temp1 = "c"
    var temp2 = UInt8(UInt32(r'c'))
    var res1 = a1.indexOf(temp1, <!TYPE_MISMATCH!>"12"<!>)
    var res2 = a1.indexOf(temp2, <!TYPE_MISMATCH!>"12"<!>)
    return 0
}
