// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output %run_args

class RGBColorIterator <: Iterator<RGBColor> {
    public var index = -1
    public func next(): Option<RGBColor> {
        index++
        match (index) {
            case 0 => RGBColor.Red
            case 1 => RGBColor.Green
            case 2 => RGBColor.Blue
            case _ => None
        }
    }
}

enum RGBColor <: Iterable<RGBColor> & ToString {
    Red | Green | Blue

    public func iterator(): Iterator<RGBColor> {
        return RGBColorIterator()
    }

    public func toString() {
        match (this) {
            case Red => "Red!\n"
            case Green => "Green!\n"
            case Blue => "Blue!\n"
        }
    }
}

main() {
    let rgbColor = RGBColor.Red
    var i = 0
    for (color in rgbColor) {
        var str = match (i) {
            case 0 => "Red!\n"
            case 1 => "Green!\n"
            case 2 => "Blue!\n"
            case _ => ""
        }
        i++
        if (color.toString() != str) {
            return i
        }
    }
    return 0
}
