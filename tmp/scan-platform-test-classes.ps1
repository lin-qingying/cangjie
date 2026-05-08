$base = 'C:\Users\lin17\.gradle\caches\8.10.2\transforms\2a729307042cd25928cb5d7a8d4f4b1c\transformed\ideaIC-2024.3-win'
Get-ChildItem -Recurse $base -Filter *.jar | ForEach-Object {
  $jar = $_.FullName
  $entries = & jar tf $jar 2>$null
  if ($entries -match 'BasePlatformTestCase.class' -or $entries -match 'LightPlatformCodeInsightFixtureTestCase' -or $entries -match 'TempFiles.class' -or $entries -match 'IdeaTestUtil.class') {
    Write-Output "JAR: $jar"
    if ($entries -match 'BasePlatformTestCase.class') { Write-Output '  BasePlatformTestCase' }
    if ($entries -match 'LightPlatformCodeInsightFixtureTestCase') { Write-Output '  LightPlatformCodeInsightFixtureTestCase' }
    if ($entries -match 'TempFiles.class') { Write-Output '  TempFiles' }
    if ($entries -match 'IdeaTestUtil.class') { Write-Output '  IdeaTestUtil' }
  }
}
