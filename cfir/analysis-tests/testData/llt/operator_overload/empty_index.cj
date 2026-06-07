// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct LA{
  public var a : (Array<Bool>, Array<Bool>)
  public init(input:Bool){
    a = ([input],[input])
  }
}

main(): Int64 {
    var l_a = LA(false)
    var r_a = true
    var r_b = true
    l_a.a[0][<!parse_expected_expression!>]<!> = r_a || r_b
 0   <!parse_expected_character!>if<!> (l_a.a[0][0] == true) {
        return 0
    }
    return 1
}
