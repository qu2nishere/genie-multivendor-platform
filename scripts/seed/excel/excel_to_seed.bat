@echo off
rem Keo tha file .xlsx vao day de build + nap seed vao may dev (plan 2026-11-21 E01.03).
rem Khong keo file nao thi dung scripts\seed\excel\products.xlsx. Huong dan: README.md cung thu muc.
setlocal
chcp 65001 >nul
if "%~1"=="" (set "XLSX=%~dp0products.xlsx") else (set "XLSX=%~1")
where python >nul 2>nul && (set "PY=python") || (set "PY=py -3")
%PY% "%~dp0excel_to_seed.py" build "%XLSX%" --apply --flush-cache
echo.
echo Exit code: %ERRORLEVEL%
pause
