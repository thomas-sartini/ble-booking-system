$ErrorActionPreference = 'Stop'

foreach ($program in @('pandoc', 'xelatex')) {
    if (-not (Get-Command $program -ErrorAction SilentlyContinue)) {
        throw "$program is missing. See docs/README.md."
    }
}

$defaults = Join-Path $PSScriptRoot 'report/pandoc.yaml'
$output = Join-Path $PSScriptRoot 'output/IIP_BLE_Booking_System.pdf'
New-Item -ItemType Directory -Force (Split-Path $output) | Out-Null
$tmp = Join-Path $PSScriptRoot 'output/.tmp'
New-Item -ItemType Directory -Force $tmp | Out-Null
$oldTmp = $env:TMP
$oldTemp = $env:TEMP
try {
    $env:TMP = $tmp
    $env:TEMP = $tmp
    & pandoc --defaults $defaults
} finally {
    $env:TMP = $oldTmp
    $env:TEMP = $oldTemp
}
if ($LASTEXITCODE -ne 0) { throw "Pandoc build failed (exit code $LASTEXITCODE)." }
Write-Host "PDF created: $output"
