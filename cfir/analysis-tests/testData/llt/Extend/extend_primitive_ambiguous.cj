// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

extend Int32 {
    func b() {}
}

extend Int8 {
    func b() {}
}

var a = 1.<!AMBIGUOUS_MATCH_PRIMITIVE_EXTEND!>b<!>()
