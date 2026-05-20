// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: provider.cj
package completion.provider

public func needsImport(): Int64 {
    return 1
}

// FILE: hidden.cj
package completion.consumer

private func hiddenCandidate(): Int64 {
    return 2
}

// FILE: consumer.cj
package completion.consumer

func directCandidate(): Int64 {
    return 3
}

func consume(): Int64 {
    return completion.consumer.directCandidate()
}
