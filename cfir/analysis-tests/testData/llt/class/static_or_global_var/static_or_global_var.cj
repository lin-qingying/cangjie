// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

var a = B.<!USED_BEFORE_INITIALIZATION!>a<!>
var b = A.<!USED_BEFORE_INITIALIZATION!>b<!>
var d = 1

class B {
    public static var a: Int64 = A.<!USED_BEFORE_INITIALIZATION!>b<!>
    public static var c = d
}

class A {
    public static var b: Int64 = B.a
}
