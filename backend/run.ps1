$envFile = Join-Path $PSScriptRoot ".env"

if (-not (Test-Path $envFile)) {
    Write-Host "No .env file found at $envFile" -ForegroundColor Red
    Write-Host "Copy .env.example to .env first: cp .env.example .env" -ForegroundColor Yellow
    exit 1
}

Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "" -or $line.StartsWith("#")) {
        return
    }
    $parts = $line -split "=", 2
    if ($parts.Length -eq 2) {
        $name = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "Set $name" -ForegroundColor DarkGray
    }
}

Write-Host "Starting backend..." -ForegroundColor Green
mvn spring-boot:run
