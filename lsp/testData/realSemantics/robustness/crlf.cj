package sample.crlf

func anchor(): Int64 {
    return 1
}

func broken(): Int64 {
    return "text"
}

func caller(): Int64 {
    return anchor()
}
