import semanticMember.AddGeneratedMember

@AddGeneratedMember
class GeneratedHost {}

func useGeneratedClassMember(): Int64 {
    let value = GeneratedHost()
    return value.generatedMember()
}

main(): Int64 {
    return useGeneratedClassMember()
}
