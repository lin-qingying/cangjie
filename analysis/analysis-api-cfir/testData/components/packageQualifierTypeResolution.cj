// MODULE: main
// MAIN_FILE_NAME: consumer.cj
// FILE: provider.cj
package completion.provider

public class ProviderType {}

// FILE: consumer.cj
package completion.consumer

import completion.provider

func validImportedType(value: provider.ProviderType): Unit {}

func invalidUnimportedType(value: completion.provider.ProviderType): Unit {}
