@echo off
REM Compila o backend e corre todos os testes (unitarios + integracao da API com H2).
cd /d "%~dp0backend"
call "%~dp0backend\mvnw.cmd" -B test
exit /b %errorlevel%
