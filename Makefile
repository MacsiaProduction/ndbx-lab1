ENV_FILE = .env.local
DC = docker compose --env-file $(ENV_FILE)

.DEFAULT_GOAL = help

.PHONY: help
help:
	@echo "run      — запустить все сервисы в фоне"
	@echo "rund     — запустить в foreground (вывод в терминал)"
	@echo "stop     — остановить сервисы"
	@echo "clean    — остановить сервисы и удалить volumes"
	@echo "logs     — вывод логов (Ctrl+C для выхода)"
	@echo "services — статус запущенных контейнеров"

.PHONY: run
run:
	$(DC) up -d --build

.PHONY: rund
rund:
	$(DC) up --build

.PHONY: stop
stop:
	$(DC) down

.PHONY: clean
clean:
	$(DC) down -v

.PHONY: logs
logs:
	$(DC) logs -f

.PHONY: services
services:
	$(DC) ps
