@echo off
echo.
echo #################################################################
echo #                                                               #
echo #      SETUP: Demo 4 - Invasor Simulado (Guerra Hacker)         #
echo #                                                               #
echo #################################################################
echo.
echo Este script vai preparar o cenario de ataque.
echo.
echo ETAPA 1: O "invasor" inicia um container malicioso...
echo -----------------------------------------------------------------
echo.
docker rm -f crypto-miner-instance > nul 2>&1
docker run -d --name crypto-miner-instance alpine sh -c "while true; do sleep 5; done"
echo.
echo Container 'crypto-miner-instance' iniciado.
echo.
timeout /t 3 /nobreak > nul

echo.
echo ETAPA 2: O sistema de monitoramento (simulado) detecta a anomalia...
echo -----------------------------------------------------------------
echo.
docker exec redis redis-cli HSET fact-base malicious_containers_present true
echo.
echo Fato 'malicious_containers_present' definido como 'true' no Redis.
echo.
echo #################################################################
echo #                                                               #
echo #      CENARIO PRONTO. EXECUTE A ETAPA FINAL MANUALMENTE.       #
echo #                                                               #
echo #################################################################
echo.
echo O ataque foi simulado. Agora, copie e execute o comando abaixo
echo em seu terminal para acionar a resposta do AOE:
echo.
echo -----------------------------------------------------------------
echo.
echo curl -X POST http://localhost:8080/api/goals -H "Content-Type: application/json" -d "{ \"name\": \"neutralize-threat\", \"priority\": 1, \"desiredState\": { \"malicious_containers_present\": false } }"
echo.

echo -----------------------------------------------------------------
echo.
Apos executar o comando, observe os logs do 'aoe-planner' e
'aoe-executor' para ver a resposta.
echo.