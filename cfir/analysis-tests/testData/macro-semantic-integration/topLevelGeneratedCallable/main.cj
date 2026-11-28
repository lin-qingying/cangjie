import semanticTop.GenerateTopLevel

@GenerateTopLevel
func macroCarrier(): Unit {}

func useGeneratedTopLevelCallable(): Int64 {
    let value = generatedTopLevel(42)
    return value
}

main(): Int64 {
    return useGeneratedTopLevelCallable()
}
