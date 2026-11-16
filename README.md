# PsiCare - Sistema de Atendimento Psicológico

Plataforma web para acompanhamento psicológico com cadastro/login, diário do paciente (texto, ditado e áudio), vínculo paciente–psicólogo, testes psicológicos e vocacionais, e gestão de conta/segurança.

> **Uso rápido (recomendado):** veja **[COMO-USAR.md](COMO-USAR.md)**. Com o Docker Desktop instalado, execute `iniciar.bat` (Windows) ou `./iniciar.sh` (Linux/Mac) e abra http://localhost:3000.  
> Instalação/atualização automática (tenta instalar Docker, baixar do GitHub e, se já existir instalação, atualizar para a versão mais recente): [instalar-e-rodar.bat](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat) | [instalar-e-rodar.sh](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh)

## Acesso padrão
- **Sistema:** http://localhost:3000
- **API:** http://localhost:8080 (saúde: `/actuator/health`)
- **Administrador padrão:** `admin@psicare.com` / `Admin@123` (altere em **Conta → Alterar senha**)

## Execução com Docker (1 comando)
Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado e em execução.

```bash
docker compose up --build -d
```
Atalhos:
- **Windows:** `iniciar.bat` (parar: `parar.bat`)
- **Linux/Mac:** `./iniciar.sh` (parar: `./parar.sh`)

Sobe automaticamente: PostgreSQL + API (Spring Boot) + Web (React/Nginx).

## Stack
- **Backend:** Java 17, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway
- **Frontend:** React + TypeScript + Vite, Tailwind CSS, React Router, TanStack Query, Axios
- **Infra:** Docker Compose (inclui volume para uploads/anexos)

## Funcionalidades implementadas
- **Autenticação e perfis:** registro aberto, login JWT, convites opcionais, perfis `PATIENT`, `PSYCHOLOGIST`, `ADMIN`, administrador padrão semeado.
- **Diário completo:** criar/editar/excluir registros (texto, título, humor, data), ditado por voz (Web Speech API, quando disponível), gravação/upload/reprodução/remoção de áudio e anexos, busca e filtro por humor, organização por mês, comentários/feedback do profissional.
- **Painel do psicólogo e vínculo:** vincular pacientes (por e-mail), visualizar diários/áudios/evolução dos vinculados e registrar feedback; auto-vínculo quando paciente se cadastra via convite criado por psicólogo.
- **Motor de testes:** testes psicológicos e vocacionais com questões/opções, iniciar/continuar tentativa, salvar progresso, finalizar com pontuação e resumo, histórico de tentativas.
- **Conta e segurança:** atualização de perfil (nome) e alteração de senha.

## Execução para desenvolvimento
1. Banco: `docker compose up -d postgres` (ou via compose completo).
2. Backend: `cd backend && mvn spring-boot:run` (http://localhost:8080). Configurações em `backend/src/main/resources/application.yml` (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `APP_STORAGE_UPLOAD_DIR`, `JWT_SECRET`).
3. Frontend: `cd frontend && npm install && npm run dev` (http://localhost:5173, proxy `/api`).

## API (principais endpoints)
- **Auth:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/me`, `POST /api/auth/change-password`, `POST /api/auth/invites`
- **Diário (paciente):** `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}`, `POST /api/journal/{id}/attachments`, `GET/DELETE /api/journal/attachments/{id}` (+ `/file`)
- **Psicólogo:** `GET /api/psychologist/patients`, `POST /api/psychologist/patients/link`, `GET /api/psychologist/patients/{patientId}/journal`, `POST /api/psychologist/journal/{entryId}/comments`, `GET /api/psychologist/attachments/{id}/file`
- **Testes:** `GET /api/tests`, `GET /api/tests/{id}`, `POST /api/tests/{id}/attempts`, `GET /api/attempts/me`, `GET/POST /api/attempts/{id}/answers`, `POST /api/attempts/{id}/submit`

## Estrutura
- `backend/` — API REST Spring Boot
- `frontend/` — aplicação React
- `docs/ARQUITETURA.md` — arquitetura e roadmap
- `docker-compose.yml` — execução completa (db + backend + frontend)
- `iniciar.*` / `parar.*` — execução
- `instalar-e-rodar.*` — instalação, atualização e execução automática
- `COMO-USAR.md` — guia para usuários

## Segurança, operação e observações
- **Dados sensíveis (LGPD/ética):** antes de produção, revise consentimento/privacidade, políticas de retenção e acesso, e garanta HTTPS, segredos fortes (`JWT_SECRET`, `SPRING_DATASOURCE_PASSWORD`), troca da senha padrão do administrador, backups e hardening.
- **Segurança implementada (base):** autenticação JWT, autorização por perfil/recurso (vínculo paciente–psicólogo), headers de segurança, CORS configurável (`APP_CORS_ALLOWED_ORIGIN_PATTERNS`), e auditoria de acessos a dados clínicos sensíveis (ex.: visualização de diário/anexos por psicólogo).
- **Operação:** health check em `/actuator/health` (detalhes desabilitados por padrão); paginação opcional em listagens de diário via `?page=&size=`; limites de upload configurados.
- **Testes automatizados:** backend (JUnit/MockMvc, perfil de teste com H2) em `backend/src/test` (`mvn test`); frontend (Vitest + Testing Library) em `frontend` (`npm test`).
- **Interpretação clínica:** resultados de testes são instrumentos de apoio e devem ser interpretados por profissional qualificado.
