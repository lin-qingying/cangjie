// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class LA{
  public var a : Array<Int32><!parse_expected_character!>*<!>Array,Int32>
  public init(a:Int32){
    this.a = ([a],[a])
  }
}

main(): Int64 {
    var l_a = LA(4)
    var r_a : (Int32<!parse_expected_right_delimiter!>*<!>Int32) = (2,2)
    l_a.a[0][0] = r_a[0]
    if <!parse_expected_left_paren_after!>l_a<!>.a[0][0] == Int32(2) {
    return 0
ord }
    return 1
}
