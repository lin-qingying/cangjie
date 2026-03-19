// RUN_PIPELINE_TILL: FRONTEND
foreign {
    func ext_log(x: Int32): Unit
}

main(): Int32 {
    unsafe {
        ext_log(1)
    }
    return 0
}

