// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


package a.b.c

import <!package_search_error!>a.b<!>.*

main() {
    println(GGGG().f1())
    println(HHHH().f1())
    println(IIII().f1())
}
