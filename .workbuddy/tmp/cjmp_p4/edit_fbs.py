import re
p = 'D:/code/intellij/cangjie/flatbuffers-gen/flatbuffers/ModuleFormat.fbs'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
old_start = s.index('// CJMP: file-level features directive.')
old_end = s.index('// all SemaTys, decls are saved here')
new = '''// CJMP: file-level features directive. Layout is byte-identical to official
// origin/main `schema/CjoFormat.fbs` (FeatureId / FeaturesSet / FeaturesDirective).
table FeatureId {
  identifiers: [string]; // ["os", "linux"]
}

table FeaturesSet {
  features: [FeatureId];
}

table FeaturesDirective {
  featuresSet: FeaturesSet;
}

// CJMP: compile options embedded in the common part cjo (official
// `ASTLoader::ValidateOptions`). Layout is byte-identical to official
// `CompilationOptions` (slot 0 = optimization_level, slot 1 = debug).
enum OptimizationLevel : uint8 {
  O0, O1, O2, O3, Os, Oz
}

table CompilationOptions {
  optimization_level: OptimizationLevel;
  debug: bool;
}

'''
s = s[:old_start] + new + s[old_end:]
s = s.replace('''  // compile options embedded in the common part cjo for CJMP validation.
  options: Option;''', '''  // compile options embedded in the common part cjo for CJMP validation.
  options: CompilationOptions;''')
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)
print('ok')
