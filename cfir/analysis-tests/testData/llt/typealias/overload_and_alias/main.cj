// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: a
// EXEC: %compiler %cmp_opt %compile_lib_opt a/a.cj -o a.%middle
// EXEC: %compiler %cmp_opt a.%middle %n.cj --import-path a -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

import std.time.DateTime
import a.*

public func maxDateTime(dateList:List<DateTime>):DateTime{
    return DateTime.now();
}

public func testFunc(tradeDate1: DateTime) {
    return List(maxDateTime(List(tradeDate1)))
}

main(): Int64 {
    return 0
}
