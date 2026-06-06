// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class R{

}

extend R {
    public static func a2() : <!parse_this_type_not_allow!>This<!>{

    }

	func foo(): <!parse_this_type_not_allow!>This<!> {
		return this
	}
}

main(): Int64 {
    return 0
}
