// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    if ( C().a != 1 ){
        return 1
    }
    return 0
}

open class A<T> {
    public func foo(){
        return 1
    }
}

class C <: A<Int6<!parse_expected_left_brace, parse_expected_right_delimiter!>
<!>
