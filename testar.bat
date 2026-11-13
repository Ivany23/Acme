@echo off
REM Corre TODOS os testes automatizados: backend (Maven) e frontend (Vitest).
setlocal
echo ===== Backend: unidade, servicos, integracao e sistema =====
cd /d "%~dp0backend"
call "%~dp0backend\mvnw.cmd" -B -ntp test || goto :falhou

echo.
echo ===== Frontend: unidade, servicos, integracao e sistema =====
cd /d "%~dp0frontend"
if not exist node_modules call npm ci || goto :falhou
call npm test || goto :falhou

echo.
echo TODOS OS TESTES PASSARAM.
exit /b 0

:falhou
echo.
echo HA TESTES A FALHAR.
exit /b 1
