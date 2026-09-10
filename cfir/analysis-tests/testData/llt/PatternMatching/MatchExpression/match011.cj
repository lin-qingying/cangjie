

func test(): Int64 {
    let x: Int64 = 1;
    let r: Int64 = match (x){
        case <!UNREACHABLE_PATTERN!>0<!> => 1
        case 1 => 0
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
    return r
}

main(): Int64 {
    return test()
}
