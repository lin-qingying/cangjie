// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() : Int64 {
    return 0
}

type A = <!parse_this_type_not_allow!>This<!>

extend A {
    public operator func ==(right:Rune):Bool  {
        if (right == 'a') {
            return true
        }else{
            return false
        }
    }
}
