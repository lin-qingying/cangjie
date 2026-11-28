package completion.sections

enum Flag {
    | On
    | Off
}

func enumKindSite(): Int64 {
    Flag.completionTarget
}