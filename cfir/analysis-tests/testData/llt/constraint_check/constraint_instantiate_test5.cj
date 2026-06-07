// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class X {}

interface Y {}

class R<T> where T <: X & Y {}

var a = <!GENERIC_ARGUMENT_NO_MATCH!>R<X>()<!>
var b = <!GENERIC_ARGUMENT_NO_MATCH!>R<Y>()<!>

main(): Int64 {
    return 0
}
