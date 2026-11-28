// RUN_PIPELINE_TILL: FRONTEND
foreign func puts(s: CString): Int32

foreign {
    func getpid(): Int32
}

func callForeign(): Int32 {
    return unsafe {
        0
    }
}
