@echo off
REM Arranca a API em http://localhost:8080 (le as credenciais da Neon em backend\.env).
cd /d "%~dp0backend"
if not exist .env (
  echo Falta o ficheiro backend\.env. Copie backend\.env.example para backend\.env e preencha.
  exit /b 1
)
call "%~dp0backend\mvnw.cmd" spring-boot:run
