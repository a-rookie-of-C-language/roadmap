# PostGIS Installation Script - Copy DLLs to PostgreSQL directory
# Must be run as Administrator

$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Definition
$source = Join-Path $scriptRoot "..\postgis-tmp\postgis-bundle-pg18-3.6.1x64" | Resolve-Path -ErrorAction SilentlyContinue | ForEach-Object { $_.Path }
# Default target (PostgreSQL installation). You can override by setting the TARGET environment variable before running.
if ($env:TARGET) { $target = $env:TARGET } else { $target = "D:\pgsql" }

Write-Host "Copying PostGIS bin DLLs..."
Copy-Item -Path "$source\bin\*.dll" -Destination "$target\bin\" -Force -ErrorAction Stop
Write-Host "Copied bin DLLs successfully."

Write-Host "Copying PostGIS bin EXEs..."
Copy-Item -Path "$source\bin\*.exe" -Destination "$target\bin\" -Force -ErrorAction SilentlyContinue
Write-Host "Copied bin EXEs."

Write-Host "Copying PostGIS lib files..."
Copy-Item -Path "$source\lib\*.dll" -Destination "$target\lib\" -Force -ErrorAction SilentlyContinue
Write-Host "Copied lib DLLs."

Write-Host "Copying PostGIS share/extension files..."
Copy-Item -Path "$source\share\extension\*" -Destination "$target\share\extension\" -Force -ErrorAction SilentlyContinue
Write-Host "Copied share/extension files."

Write-Host "Copying PostGIS share/contrib files..."
if (Test-Path "$source\share\contrib") {
    Copy-Item -Path "$source\share\contrib\*" -Destination "$target\share\contrib\" -Force -Recurse -ErrorAction SilentlyContinue
}
Write-Host "Done."

Write-Host ""
Write-Host "Verifying key files..."
$files = @(
    "$target\bin\libgeos_c.dll",
    "$target\bin\libgeos.dll",
    "$target\bin\libproj_8_2.dll",
    "$target\lib\postgis-3.dll"
)
foreach ($f in $files) {
    if (Test-Path $f) {
        Write-Host "  OK: $f"
    } else {
        Write-Host "  MISSING: $f" -ForegroundColor Red
    }
}
Write-Host "PostGIS installation complete."
