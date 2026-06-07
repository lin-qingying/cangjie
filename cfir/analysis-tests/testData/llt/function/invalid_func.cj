// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func test(): Data{
    Data#<!lex_expected_quote_in_raw_string!>(<!>)
}

func> main(): Int64 {
    test()
    return 0
}
