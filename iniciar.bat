@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"
title Almox TI

echo ==========================================================
echo   Almox TI - Almoxarifado de TI e Manutencao
echo ==========================================================
echo.

rem ---- 1. O Java esta instalado? ----
where java >nul 2>nul
if errorlevel 1 goto semjava

rem ---- 2. A versao do Java e 21 ou mais nova? ----
set "VERSAO="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do if not defined VERSAO set "VERSAO=%%~v"

if not defined VERSAO goto versaoantiga

set "MAIOR=!VERSAO!"
for /f "delims=.-_+" %%a in ("!VERSAO!") do set "MAIOR=%%a"

if not defined MAIOR goto versaoantiga

set "NAO_NUMERICO="
for /f "delims=0123456789" %%a in ("!MAIOR!") do set "NAO_NUMERICO=%%a"
if defined NAO_NUMERICO goto versaoantiga

echo Verificando o Java... encontrado na versao !VERSAO!.
if !MAIOR! LSS 21 goto versaoantiga

echo Tudo pronto. O sistema vai abrir em: http://localhost:8080
echo (Para encerrar, feche esta janela ou pressione Ctrl+C.)
echo.

rem ---- 3. Jar pronto na pasta? Usa ele. Senao, roda pelo codigo-fonte. ----
if exist "almox-ti.jar" (
    echo Usando o arquivo almox-ti.jar desta pasta...
    echo.
    java -jar almox-ti.jar
    goto fim
)

if not exist "mvnw.cmd" (
    echo [ERRO] Nenhum almox-ti.jar encontrado e o projeto Maven nao esta aqui.
    echo        Baixe o almox-ti.jar em:
    echo        https://github.com/twkryan/spring-boot-almoxarifado/releases/latest
    echo        e coloque-o nesta pasta.
    goto erro
)

echo Compilando e iniciando pelo codigo-fonte ^(a primeira vez demora um pouco^)...
echo.
call mvnw.cmd spring-boot:run
goto fim

:semjava
echo [ERRO] O Java nao foi encontrado neste computador.
echo.
echo Instale o Java 21 ^(Temurin^) e depois rode este arquivo novamente:
echo   https://adoptium.net/temurin/releases/?version=21
echo.
echo Dica: durante a instalacao, marque a opcao "Set JAVA_HOME variable".
goto erro

:versaoantiga
echo [ERRO] A versao do Java instalada nao serve. O Almox TI precisa do Java 21 ou mais novo.
echo.
echo Versao encontrada: !VERSAO!
echo Baixe o Java 21 ^(Temurin^) em:
echo   https://adoptium.net/temurin/releases/?version=21
echo.
echo Dica: durante a instalacao, marque a opcao "Set JAVA_HOME variable".
goto erro

:erro
echo.
pause
endlocal
exit /b 1

:fim
echo.
echo O sistema foi encerrado.
pause
endlocal
exit /b 0
