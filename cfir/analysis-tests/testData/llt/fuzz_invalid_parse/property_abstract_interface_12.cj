
// Was segv, should quit normally
interface A{
    prop<!parse_expected_name!>@<!>b<!parse_expected_decl!>:<!> Int64
}

struct B <: A{
    public prop b: Int64{
        get(){
            0
        }
    }
}

main():Int64 {
    let a = B()
    return a.b
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
