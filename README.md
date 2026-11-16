# PsiCare - Sistema de Atendimento Psicológico

Plataforma web para acompanhamento psicológico com cadastro/login, diário do paciente (texto e ditado por voz), canal de testes e suporte a convites.

> **Para uso rápido (recomendado, inclusive para pessoas leigas):** veja o guia **[COMO-USAR.md](COMO-USAR.md)**. Com o Docker Desktop instalado, basta executar `iniciar.bat` (Windows) ou `./iniciar.sh` (Linux/Mac) e abrir http://localhost:3000.

## Execução fácil com Docker (1 comando)
Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e em execução.

```bash
docker compose up --build -d
```
Ou use os atalhos:
- **Windows:** `iniciar.bat` (para parar: `parar.bat`)
- **Linux/Mac:** `./iniciar.sh` (para parar: `./parar.sh`)

Acesse: **http://localhost:3000**

Isso sobe automaticamente: PostgreSQL + API (Spring Boot) + Web (React/Nginx).

## Stack
- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway
- **Frontend:** React + TypeScript + Vite, Tailwind CSS, React Router, TanStack Query, Axios
- **Infra:** Docker Compose

## Funcionalidades (MVP)
- **Autenticação:** registro aberto (padrão paciente) e login com JWT
- **Convites:** geração de convite (`POST /api/auth/invites`) e cadastro com código de convite
- **Perfis:** `PATIENT`, `PSYCHOLOGIST`, `ADMIN` (base preparada)
- **Diário do paciente:** criar, listar e excluir anotações (texto, título, humor, data) com histórico e ditado por voz no navegador (Web Speech API, quando disponível)
- **Testes:** tela/canal de testes psicológicos e vocacionais e base de dados para evolução do motor de aplicação
- **UI:** telas responsivas e estilizadas com Tailwind

## Execução para desenvolvimento (sem Docker completo da aplicação)
1. Banco de dados: `docker compose up -d postgres` (ajuste a porta/URL se necessário) ou use o `docker-compose.yml` conforme seu ambiente.
2. Backend: `cd backend && mvn spring-boot:run` (http://localhost:8080). Configurações em `backend/src/main/resources/application.yml` (suporta `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`).
3. Frontend: `cd frontend && npm install && npm run dev` (http://localhost:5173 com proxy `/api`).

## API (principais endpoints)
- `POST /api/auth/register` — cadastro (`name`, `email`, `password`, `inviteCode?`, `role?`)
- `POST /api/auth/login` — login (`email`, `password`)
- `GET /api/auth/me` — usuário autenticado
- `POST /api/auth/invites` — criar convite (autenticado)
- `GET /api/journal` — listar entradas
- `POST /api/journal` — criar entrada (`content`, `title?`, `mood?`, `entryDate?`)
- `PUT /api/journal/{id}` — atualizar entrada
- `DELETE /api/journal/{id}` — remover entrada

## Estrutura
- `backend/` — API REST Spring Boot
- `frontend/` — aplicação React
- `docs/ARQUITETURA.md` — arquitetura e roadmap
- `docker-compose.yml` — execução completa (db + backend + frontend)
- `iniciar.*` / `parar.*` — atalhos de execução

## Observações
- Sistema lida com dados sensíveis. Antes de uso em produção, revise segurança, privacidade (LGPD), ética profissional, HTTPS, segredos e backups (o segredo JWT padrão é apenas para desenvolvimento).
