# PsiCare - Sistema de Atendimento Psicológico (MVP)

Plataforma web para acompanhamento psicológico com cadastro/login, diário do paciente (texto e ditado por voz), canal de testes e base para convites.

## Stack
- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway
- **Frontend:** React + TypeScript + Vite, Tailwind CSS, React Router, TanStack Query, Axios
- **Infra:** Docker Compose (PostgreSQL)

## Funcionalidades incluídas neste MVP
- **Autenticação:** registro aberto (padrão paciente) e login com JWT
- **Convites:** endpoint para geração de convite (`POST /api/auth/invites`, autenticado) e cadastro com código de convite (`inviteCode` no registro / tela de cadastro com opção "Tenho um código de convite")
- **Perfis:** `PATIENT`, `PSYCHOLOGIST`, `ADMIN` (base preparada; regras finas de vínculo/painel do psicólogo evoluem nas próximas etapas)
- **Diário do paciente:** criar, listar e excluir anotações (texto, título, humor, data) com histórico
- **Voz:** ditado por voz no navegador (Web Speech API, quando suportada) para preencher a anotação; estrutura de banco para anexos de áudio preparada (`journal_attachments`)
- **Testes:** tela/canal de testes psicológicos e vocacionais (UI) e migração com catálogo inicial de exemplos
- **UI:** telas responsivas e estilizadas com Tailwind (login, cadastro, dashboard, diário, testes)

## Estrutura
- `backend/` - API REST Spring Boot
- `frontend/` - Aplicação React
- `docs/ARQUITETURA.md` - arquitetura e roadmap
- `docker-compose.yml` - PostgreSQL

## Pré-requisitos
- Java 21+ e Maven 3.9+ (ou adicione o Maven Wrapper no backend)
- Node.js 18+ e npm
- Docker e Docker Compose

## Como executar (desenvolvimento)

### 1. Banco de dados
```bash
docker compose up -d
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
```
- API: `http://localhost:8080`
- Migrações Flyway executam automaticamente
- Configurações padrão em `backend/src/main/resources/application.yml` (DB: `psicology/psicology`, JWT com segredo de desenvolvimento — **alterar em produção**)

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
- App: `http://localhost:5173`
- Proxy de `/api` para `http://localhost:8080` configurado no Vite

## Fluxos principais
1. **Cadastro aberto:** acesse "Cadastre-se" e crie uma conta de paciente.
2. **Cadastro por convite:** na tela de cadastro, marque "Tenho um código de convite" e informe o código (ou acesse com `?convite=CODIGO`). O perfil pode ser definido pelo convite.
3. **Gerar convite:** com um usuário autenticado, envie `POST /api/auth/invites` com JSON `{"email":"opcional@exemplo.com","role":"PATIENT|PSYCHOLOGIST|ADMIN"}` e header `Authorization: Bearer <token>`.
4. **Diário:** após login, acesse "Meu Diário" para escrever ou ditar anotações e consultar o histórico.

## API (principais endpoints)
- `POST /api/auth/register` — cadastro (`name`, `email`, `password`, `inviteCode?`, `role?`)
- `POST /api/auth/login` — login (`email`, `password`)
- `GET /api/auth/me` — usuário autenticado
- `POST /api/auth/invites` — criar convite (autenticado)
- `GET /api/journal` — listar entradas do paciente autenticado
- `POST /api/journal` — criar entrada (`content`, `title?`, `mood?`, `entryDate?`)
- `PUT /api/journal/{id}` — atualizar entrada
- `DELETE /api/journal/{id}` — remover entrada

## Próximas evoluções recomendadas
- Upload e reprodução de áudio anexado às entradas do diário (backend storage + UI)
- Motor completo de aplicação de testes (perguntas, tentativas, respostas, cálculo e relatórios)
- Vínculo paciente↔psicólogo e painel do psicólogo para avaliação de diários/testes
- Refresh token, recuperação de senha, verificação de e-mail
- Hardening de segurança/LGPD (consentimento, auditoria, criptografia em repouso, políticas de retenção)
- Testes automatizados e observabilidade

> **Aviso:** este sistema lida com dados sensíveis. Antes de uso em produção, revise requisitos de segurança, privacidade (LGPD), ética profissional e infraestrutura (HTTPS, segredos, backups).
