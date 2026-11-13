@echo off
REM Arranca a API (http://localhost:8080) e o frontend (http://localhost:5173) em duas janelas.
REM Le as credenciais da Neon em backend\.env
if not exist "%~dp0backend\.env" (
  echo Falta o ficheiro backend\.env. Copie backend\.env.example para backend\.env e preencha.
  exit /b 1
)
if not exist "%~dp0frontend\node_modules" (
  pushd "%~dp0frontend"
  call npm ci
  popd
)
start "Acme API" /D "%~dp0backend" cmd /k .\mvnw.cmd spring-boot:run
start "Acme Frontend" /D "%~dp0frontend" cmd /k npm run dev
echo API: http://localhost:8080   Frontend: http://localhost:5173   (utilizador admin / password acme2026)
