$base = 'C:\Users\lin17\.gradle\caches\8.10.2\transforms\2a729307042cd25928cb5d7a8d4f4b1c\transformed\ideaIC-2024.3-win'
Get-ChildItem -Recurse $base -Filter *.jar | ForEach-Object {
  $jar = $_.FullName
  $entries = & jar tf $jar 2>$null
  if ($entries -match 'LightJavaCodeInsightFixtureTestCase.class' -or $entries -match 'IdeaTestUtil.class') {
    Write-Output $jar
    if ($entries -match 'LightJavaCodeInsightFixtureTestCase.class') { Write-Output '  contains LightJavaCodeInsightFixtureTestCase' }
    if ($entries -match 'IdeaTestUtil.class') { Write-Output '  contains IdeaTestUtil' }
  }
}
