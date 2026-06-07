// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


type A = Int64

main(): A {
    var b: A = 10;
    var c: A = 100;
    var <!chir_dce_unused_variable!>d<!> = b + c;
    return 0;
}
