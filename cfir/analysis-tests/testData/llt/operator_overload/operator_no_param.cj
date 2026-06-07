// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A<T>{
    public operator func >=(<!parse_expected_name!>:<!>A<T><!parse_missing_body!>)<!>:Int16 {return 0}
}

main(): Int64 {
    return 0
}
