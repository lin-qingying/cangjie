

func test(): Int64 {
    {=> return 0}()
}

main(): Int64 {
    return test()
}
