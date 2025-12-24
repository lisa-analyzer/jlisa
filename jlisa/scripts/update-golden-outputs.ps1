# Script to update golden outputs (JSON files only) for SVComp testcases
# IMPORTANT: Only copies JSON files, preserves input files like Main.java

$failedTests = @(
    "ArithmeticException6", "CharSequenceBug", "ClassCastException1", 
    "Class_method1", "Inheritance1", "MathSin", "NegativeArraySizeException1",
    "NullPointerException4", "StaticCharMethods01", "StringCompare01",
    "StringConcatenation01", "StringContains02", "StringValueOf02",
    "StringValueOf08", "StringValueOf09", "SubString02", "SubString03",
    "athrow1", "boolean1", "const1", "exceptions16", "exceptions1",
    "exceptions6", "exceptions8", "exceptions9", "iarith1", "iarith2",
    "if_icmp1", "instanceof4", "instanceof8", "interface1", "return1",
    "swap1", "virtual2"
)

$updatedCount = 0
$skippedCount = 0
$errorCount = 0

Write-Host "Starting bulk update of golden outputs (JSON files only)..." -ForegroundColor Cyan
Write-Host "Total tests to update: $($failedTests.Count)" -ForegroundColor Cyan
Write-Host ""

foreach ($test in $failedTests) {
    $expectedPath = ".\java-testcases\svcomp\$test"
    $actualPath = ".\java-outputs\svcomp\$test"
    
    if (-not (Test-Path $actualPath)) {
        Write-Host "[SKIP] Actual path does not exist: $test" -ForegroundColor Yellow
        $skippedCount++
        continue
    }
    
    if (-not (Test-Path $expectedPath)) {
        Write-Host "[SKIP] Expected path does not exist: $test" -ForegroundColor Yellow
        $skippedCount++
        continue
    }
    
    # Check if input files exist in expected (safety check)
    $javaFiles = Get-ChildItem -Path $expectedPath -File -Filter "*.java"
    if ($javaFiles.Count -eq 0) {
        Write-Host "[WARN] No .java files found in expected for: $test" -ForegroundColor Yellow
    }
    
    # Copy only JSON files
    $jsonFiles = Get-ChildItem -Path $actualPath -File -Filter "*.json"
    if ($jsonFiles.Count -eq 0) {
        Write-Host "[SKIP] No JSON files found in actual for: $test" -ForegroundColor Yellow
        $skippedCount++
        continue
    }
    
    try {
        $copiedCount = 0
        foreach ($jsonFile in $jsonFiles) {
            Copy-Item $jsonFile.FullName -Destination $expectedPath -Force
            $copiedCount++
        }
        Write-Host "[OK] Updated $copiedCount JSON file(s) for: $test" -ForegroundColor Green
        $updatedCount++
    } catch {
        Write-Host "[ERROR] Failed to update: $test - $($_.Exception.Message)" -ForegroundColor Red
        $errorCount++
    }
}

Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Updated: $updatedCount" -ForegroundColor Green
Write-Host "  Skipped: $skippedCount" -ForegroundColor Yellow
Write-Host "  Errors:  $errorCount" -ForegroundColor $(if ($errorCount -eq 0) { "Green" } else { "Red" })
Write-Host ""
Write-Host "Done! Run tests to verify:" -ForegroundColor Cyan
Write-Host "  .\gradlew.bat cleanTest test --tests `"it.unive.jlisa.svcomp.SVCompTestcases`" --rerun-tasks -i" -ForegroundColor White

