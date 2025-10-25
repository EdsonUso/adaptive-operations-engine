@echo off
echo.
echo #################################################################
echo #                                                               #
echo #      SETUP: Demo 5 - Vazamento de Memoria (Etapa 1)         #
echo #                                                               #
echo #################################################################
echo.
echo Simulando deteccao de alto uso de memoria no 'aoe-target-app'...
echo.
docker exec redis redis-cli SET fact:service_web_memory_high true > nul
docker exec redis redis-cli SET fact:restart_did_not_fix_memory false > nul
echo Fatos iniciais definidos no Redis:
echo   - service_web_memory_high: true
echo   - restart_did_not_fix_memory: false
echo.
echo #################################################################
echo #                                                               #
echo #      CENARIO PRONTO. EXECUTE A ETAPA FINAL MANUALMENTE.       #
echo #                                                               #
echo #################################################################
echo.
echo Agora, acione o AOE com o seguinte objetivo:
echo.
echo -----------------------------------------------------------------
echo.
curl -X POST http://localhost:8080/api/goals -H "Content-Type: application/json" -d "{ \"name\": \"fix-high-memory\", \"priority\": 1, \"desiredState\": { \"service_web_memory_high\": false } }"
echo.

-----------------------------------------------------------------
echo.

Observe os logs. O plano inicial deve ser 'RestartForHighMemory'.
echo.
