# FPM-2025 Monolith Docker Build & Test Orchestrator
# This script compiles the monolith, deploys the Docker Compose stack, waits for it to become healthy, and executes the full API test suite.

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   FPM-2025 MONOLITH DOCKER DEPLOYMENT & INTEGRATION TEST" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Compile the monolith on the host to utilize local cached dependencies
Write-Host "[1/5] Compiling fpm-monolith executable using Maven..." -ForegroundColor Yellow
mvn clean package -pl fpm-monolith -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Error "Maven build failed. Cannot proceed to Docker packaging."
    Exit $LASTEXITCODE
}
Write-Host "Maven build completed successfully." -ForegroundColor Green

# 2. Check and prepare environment variables
Write-Host "[2/5] Checking environment configuration..." -ForegroundColor Yellow
if (-not (Test-Path ".env")) {
    Write-Host ".env file not found. Copying from .env.template..." -ForegroundColor Gray
    Copy-Item ".env.template" ".env"
}
# Copy the .env file to fpm-monolith so the isolated docker-compose can access it
Copy-Item ".env" "fpm-monolith/.env" -Force
Write-Host "Environment configuration verified." -ForegroundColor Green

# 3. Clean up and deploy the Docker Compose stack
Write-Host "[3/5] Starting Docker Compose services..." -ForegroundColor Yellow
docker compose -f fpm-monolith/docker-compose.yml down -v --remove-orphans
docker compose -f fpm-monolith/docker-compose.yml up --build -d
if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to start Docker Compose services."
    Exit $LASTEXITCODE
}
Write-Host "Docker Compose services launched successfully." -ForegroundColor Green

# 4. Wait for the monolith to become healthy
Write-Host "[4/5] Waiting for backend-monolith to start and report health UP..." -ForegroundColor Yellow
$healthUrl = "http://localhost:8090/actuator/health"
$maxRetries = 30
$retryIntervalSec = 3
$isHealthy = $false

for ($i = 1; $i -le $maxRetries; $i++) {
    try {
        Write-Host "Polling health check ($i/$maxRetries)..." -ForegroundColor Gray
        $response = Invoke-RestMethod -Uri $healthUrl -Method Get -TimeoutSec 3
        if ($response.status -eq "UP") {
            $isHealthy = $true
            break
        }
    }
    catch {
        # Silent ignore connection failures during startup
    }
    Start-Sleep -Seconds $retryIntervalSec
}

if (-not $isHealthy) {
    Write-Host "ERROR: Monolith failed to start or report healthy status after $($maxRetries * $retryIntervalSec) seconds." -ForegroundColor Red
    Write-Host "Checking docker container status and logs:" -ForegroundColor Yellow
    docker compose -f fpm-monolith/docker-compose.yml ps
    docker compose -f fpm-monolith/docker-compose.yml logs --tail=50 backend-monolith
    Exit 1
}

Write-Host "Backend is healthy! Monolith is ready for testing." -ForegroundColor Green

# 5. Run the Python API test suite
Write-Host "[5/5] Executing API integration tests..." -ForegroundColor Yellow
Start-Sleep -Seconds 2 # Extra grace period for DB seeds
python scratch/test_api.py

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   Orchestration run finished successfully." -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
