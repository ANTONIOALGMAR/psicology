# PsiCare — Sistema de Apoio ao Atendimento Psicológico

Bem-vindo(a). Este documento explica **o que o sistema já faz**, **como usar no dia a dia** e **o que foi construído em cada etapa**. Se você está começando agora, comece pela seção [Primeiros passos](#primeiros-passos).

> O PsiCare é uma ferramenta de apoio: **não substitui o julgamento clínico**, não faz diagnóstico automático e não deve ser usado como único canal em situações de crise.

---

## Primeiros passos

1. Com o Docker instalado e rodando, execute `iniciar.bat` (Windows) ou `./iniciar.sh` (Linux/Mac).
2. Abra **http://localhost:3000**.
3. Entre com o administrador padrão: `admin@psicare.com` / `Admin@123`.
   - Por segurança, **troque essa senha** em **Conta → Alterar senha** no primeiro acesso.
4. No menu **Administração → Usuários**, crie a conta de cada psicólogo e de cada paciente (ou deixe o paciente se cadastrar e depois vincule pelo e-mail dele).
5. Cada psicólogo entra com sua conta e vê **somente os próprios pacientes**.

Guia mais detalhado: [COMO-USAR.md](COMO-USAR.md).

---

## O que o sistema faz hoje (em linguagem simples)

### Para o psicólogo
- **Meus pacientes:** vincular pacientes pelo e-mail e acompanhar diário, áudios, evolução em testes e feedbacks registrados. Você **não vê** pacientes de outros psicólogos.
- **Agenda:** criar atendimentos com data, hora, duração e modalidade (online ou presencial). O sistema **avisa se o horário já está ocupado** e mostra apenas a sua agenda. Você pode marcar como concluído ou cancelar.
- **Transferência de cliente:** se precisar encaminhar um paciente, escolha outro psicólogo e informe o motivo. O vínculo muda, os **agendamentos futuros** vão para o novo profissional, e o diário/resultados antigos ficam no histórico. Tudo fica registrado.
- **Testes:** acompanhar resultados e comparar a evolução do paciente ao longo do tempo (sempre como apoio à interpretação profissional).
- **Privacidade:** consultar solicitações de dados dos pacientes e a trilha de auditoria (área administrativa).

### Para o paciente
- **Diário:** registrar texto, humor, data e áudio (ditado ou gravação), com organização por mês e busca.
- **Testes:** responder questionários psicológicos e vocacionais, salvar progresso e ver o resultado com linguagem de orientação.
- **Agenda:** ver os próprios atendimentos e solicitar um horário ao profissional vinculado.
- **Privacidade:** aceitar ou retirar consentimentos, baixar uma cópia dos seus dados em JSON e pedir cópia ou exclusão dos dados.

### Para o administrador
- **Painel de Administração:** visão geral, criação e gestão de usuários (perfil, ativar/desativar, redefinir senha), catálogo de testes (criar teste, questões e opções), solicitações de privacidade e trilha de auditoria.

---

## Histórico de evoluções (controle do que foi feito)

| Etapa | O que mudou na prática |
|---|---|
| Evolução 1 | Diário do paciente com texto, gravação/envio de áudio, anexos e organização por mês. |
| Evolução 2 | Vínculo paciente–psicólogo, painel do profissional e comentários no diário. |
| Evolução 3 | Motor de testes psicológicos e vocacionais com questões, respostas e resultados. |
| Evolução 4 | Conta do usuário: editar nome e alterar senha. |
| Evolução 5 | Ajustes de usabilidade, telas responsivas (celular/tablet) e documentação. |
| Evolução 6 | Testes automatizados, correções de erros e qualidade do código. |
| Evolução 7 | Segurança: acesso por perfil, proteção dos dados clínicos e auditoria de acessos. |
| Evolução 8 | Operação: health check, paginação, limites de upload e melhorias de desempenho. |
| Evolução 9 | Evolução clínica: histórico comparativo de testes e melhorias na experiência. |
| Evolução 10 | Administração e LGPD: painel administrativo, consentimentos, exportação de dados e solicitações de titulares. |
| Evolução 11 | Agenda de atendimentos com controle de horários e transferência de clientes entre psicólogos. |

> Próxima etapa planejada: **chatbot de orientação fora do horário**, com respostas prontas, aviso de que é automático e encaminhamento para canais de emergência.

---

## Como usar a agenda e a transferência (passo a passo)

1. No menu **Agenda**, informe o paciente, a data/hora, a duração e a modalidade.
2. Se o horário estiver ocupado na sua agenda, o sistema avisa — escolha outro horário.
3. O paciente vê o compromisso na agenda dele. Você pode concluir ou cancelar o atendimento.
4. Para transferir: abra **Pacientes**, selecione o paciente e clique em **Transferir cliente**.
5. Escolha o novo psicólogo, escreva o motivo e confirme. Os agendamentos futuros mudam de profissional automaticamente.

**Regras de segurança da transferência:**
- Somente o psicólogo responsável (ou um administrador) pode transferir.
- O histórico clínico não é apagado nem alterado.
- A ação fica registrada com data, ator e motivo.

---

## Acesso e execução

- **Sistema:** http://localhost:3000
- **API:** http://localhost:8080 (saúde: `/actuator/health`)
- **Administrador padrão:** `admin@psicare.com` / `Admin@123` (altere no primeiro acesso)

### Forma mais simples (Docker)
```bash
docker compose up --build -d
```
Atalhos: `iniciar.bat` / `iniciar.sh` para subir e `parar.bat` / `parar.sh` para parar.

Instalação automática (baixa o projeto, instala o Docker quando possível e atualiza uma instalação existente): [instalar-e-rodar.bat](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat) | [instalar-e-rodar.sh](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh)

### Para desenvolvedores
1. Banco: `docker compose up -d postgres`
2. API: `cd backend && mvn spring-boot:run`
3. Interface: `cd frontend && npm install && npm run dev`

---

## Segurança e cuidados com dados sensíveis

- Dados de saúde são sensíveis (LGPD). O sistema controla quem pode ver o quê: paciente vê só os próprios dados; psicólogo vê só pacientes vinculados; administrador tem acesso de gestão e auditoria.
- Todas as decisões de consentimento e ações sensíveis ficam registradas na trilha de auditoria.
- Antes de usar em produção: troque a senha padrão, defina `JWT_SECRET`, configure `APP_CORS_ALLOWED_ORIGIN_PATTERNS`, use HTTPS, faça backups e defina política de retenção/descarte de dados.
- Resultados de testes e orientações automáticas são **instrumentos de apoio** e devem ser interpretados por profissional qualificado.

Mais detalhes em [docs/OPERACAO.md](docs/OPERACAO.md) e [docs/ARQUITETURA.md](docs/ARQUITETURA.md).

---

## Principais telas

| Menu | Para quem | O que faz |
|---|---|---|
| Início | Todos | Resumo e atalhos |
| Meu Diário / Pacientes | Paciente / Psicólogo | Registros clínicos e acompanhamento |
| Testes | Paciente / Psicólogo | Aplicação e resultados de testes |
| Agenda | Todos | Agendamentos de atendimento |
| Administração | Administrador | Usuários, testes, auditoria e LGPD |
| Privacidade | Todos | Consentimentos, exportação e solicitações |
| Conta | Todos | Perfil e senha |

---

## Para quem integra (API resumida)

- **Autenticação:** `POST /api/auth/register`, `POST /api/auth/login`, `GET/PUT /api/auth/me`, `POST /api/auth/change-password`
- **Diário:** `GET/POST /api/journal`, `PUT/DELETE /api/journal/{id}`, anexos em `/api/journal/{id}/attachments`
- **Psicólogo:** `GET /api/psychologist/patients`, `POST /api/psychologist/patients/link`, `POST /api/psychologist/patients/{id}/transfer`, diários e testes do paciente
- **Agenda:** `GET/POST /api/appointments`, `PUT /api/appointments/{id}`, `POST /api/appointments/{id}/cancel`
- **Testes:** `GET /api/tests`, `POST /api/tests/{id}/attempts`, `POST /api/attempts/{id}/submit`
- **Privacidade:** `GET /api/privacy/me`, `POST /api/privacy/consents`, `POST /api/privacy/requests`, `GET /api/privacy/export`
- **Administração:** endpoints `/api/admin/**` (somente perfil `ADMIN`)

---

## Estrutura do projeto

- `backend/` — API (regras de negócio, banco de dados e segurança)
- `frontend/` — telas (React)
- `docs/` — arquitetura e operação
- `docker-compose.yml` — sobe banco, API e interface juntos
- `iniciar.*`, `parar.*`, `instalar-e-rodar.*` — atalhos de execução

## Validação

- Backend: `cd backend && mvn test`
- Frontend: `cd frontend && npm test && npm run build`
- CI: `.github/workflows/ci.yml` executa os dois em cada push/PR para `main`.
