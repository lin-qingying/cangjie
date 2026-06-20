// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package a.b.c

import <!UNRESOLVED_IMPORT!>a<!>.b.*

main() {
    println(AAAA().f1())
    println(BBBB().f1())
    println(CCCC().f1())
    println(DDDD().f1())
    println(DDDD().f2())
    println(DDDD().f3())
    println(EEEE().f1())
    println(EEEE().f2())
    println(EEEE().f3())
    println(FFFF().f1())
    println(FFFF().f2())
    println(FFFF().f3())
}
