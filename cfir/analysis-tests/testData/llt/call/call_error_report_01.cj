// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import std.collection.ArrayList

func test() {
    var metrics: ArrayList<Int64> = ArrayList<Int64>([Int64(<!UNRESOLVED_REFERENCE!>UndeclaredType<!>())])
}

main(): Int64 {
    test()
    return 0
}
