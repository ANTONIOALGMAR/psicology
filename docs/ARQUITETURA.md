# Arquitetura - Sistema de Atendimento Psicológico

## 1. Visão geral
Plataforma web para acompanhamento psicológico com:
- **Paciente:** autenticação, diário comportamental (texto e voz), acesso a testes psicológicos e vocacionais, histórico.
- **Psicólogo:** gestão de pacientes vinculados, leitura/avaliação de diários, aplicação e análise de testes, relatórios.
- **Administrador:** gestão de usuários, catálogo de testes, configurações e auditoria.

> **Atenção:** dados psicológicos são sensíveis (LGPD). O sistema deve prever consentimento, controle de acesso estrito, criptografia, auditoria e políticas de retenção.

## 2. Stack proposta
- **Backend:** Java 21 + Spring Boot 3
  - Spring Web (REST), Spring Security (JWT + RBAC), Spring Validation
  - Spring Data JPA/Hibernate, PostgreSQL
  - Flyway (migrações), Spring Cache (Caffeine/Redis futuro)
  - Actuator + Micrometer (observabilidade)
- **Frontend:** React 18 + TypeScript + Vite
  - React Router, TanStack Query, React Hook Form + Zod
  - Estilização: CSS Modules ou Tailwind CSS + design system próprio (tokens, componentes)
  - Web Speech API / MediaRecorder para captura de voz (com transcrição progressiva)
- **Banco de dados:** PostgreSQL (container Docker)
- **Infra dev:** Docker Compose; produção futura com reverse proxy (Nginx), HTTPS, backups

## 3. Módulos principais

### 3.1 Autenticação e autorização
- Cadastro e login com e-mail/senha (hash BCrypt/Argon2)
- Perfis: `PATIENT`, `PSYCHOLOGIST`, `ADMIN`
- JWT access token (curta duração) + refresh token (rotação)
- Vinculação paciente ↔ psicólogo (convite/código ou associação administrativa)
- Recuperação de senha e verificação de e-mail

### 3.2 Diário do paciente (texto e voz)
- Entradas com data/hora, humor/escala opcional, tags e conteúdo textual
- Gravação de áudio no navegador (upload de arquivo) e/ou ditado por voz (Speech-to-Text no cliente quando disponível)
- Armazenamento de áudio em storage seguro (local/S3 compatível) com referência no banco
- Privacidade: paciente controla visibilidade; psicólogo acessa apenas pacientes vinculados
- Comentários/feedback do psicólogo por entrada (opcional, conforme fluxo clínico)

### 3.3 Testes psicológicos e vocacionais
- Catálogo de instrumentos/testes (metadados, público-alvo, instruções)
- Motor de aplicação: questionários (múltipla escolha, escala Likert, aberto), paginação/seções, progresso salvo
- Tentativas/respostas do paciente e cálculo de resultados (regras configuráveis por teste)
- Relatórios para psicólogo (pontuação, interpretação parametrizada, histórico comparativo)
- Separação entre testes psicológicos e testes vocacionais no catálogo e na UI

> **Nota:** aplicação de instrumentos psicológicos pode ter restrições profissionais/éticas; o sistema deve suportar controle de quem pode aplicar/visualizar cada teste.

### 3.4 Painéis e experiência
- **Paciente:** dashboard, diário, meus testes, resultados, perfil
- **Psicólogo:** lista de pacientes, timeline/diários, testes aplicados, avaliações
- UI responsiva, acessível (WCAG), visual moderno (cores calmas, tipografia legível, microinterações)

## 4. Modelo de dados (alto nível)
- `users` (id, nome, email, senha_hash, role, status, created_at)
- `patients` / `psychologists` (dados de perfil e vínculo)
- `patient_psychologist_links` (vínculo ativo, consentimento)
- `journal_entries` (patient_id, content_text, mood, visibility, created_at)
- `journal_attachments` (entry_id, type[audio], storage_key, duration, transcript?)
- `tests` (id, type[PSYCHOLOGICAL|VOCATIONAL], title, description, config/json)
- `test_questions` / `test_options`
- `test_attempts` (test_id, patient_id, status, started_at, completed_at)
- `test_answers` (attempt_id, question_id, value)
- `test_results` (attempt_id, scores/json, summary)
- `audit_logs` (ator, ação, recurso, timestamp, metadata)

## 5. API (exemplos de endpoints)
- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`
- `GET/POST /api/patients/me/journal`, `GET /api/patients/me/journal/{id}`
- `POST /api/patients/me/journal/{id}/audio` (upload)
- `GET /api/tests`, `POST /api/tests/{id}/attempts`, `POST /api/attempts/{id}/answers`, `POST /api/attempts/{id}/submit`
- `GET /api/psychologist/patients`, `GET /api/psychologist/patients/{id}/journal`, `GET /api/psychologist/attempts/{id}/result`

## 6. Performance e escalabilidade
- Paginação e filtros em listagens (diário, testes, pacientes)
- Índices em chaves de busca (patient_id, created_at, vínculos)
- Pool de conexões (HikariCP) e consultas otimizadas (evitar N+1)
- Cache de catálogo de testes e dados estáticos
- Upload de áudio assíncrono e limites de tamanho; CDN/static otimizado no frontend
- Build otimizado (code splitting, lazy routes) e assets comprimidos

## 7. Segurança e conformidade
- HTTPS, CORS restrito, headers de segurança
- Validação de entrada, proteção contra OWASP Top 10
- Autorização por recurso (paciente só acessa próprios dados; psicólogo só vinculados)
- Criptografia em trânsito e, quando aplicável, em repouso para campos/arquivos sensíveis
- Logs de auditoria para acessos a dados clínicos
- Backups e política de retenção/descartes

## 8. Roadmap de implementação sugerido
1. **Fundação:** scaffold Spring Boot + React, Docker/Postgres, autenticação JWT e perfis
2. **Diário:** CRUD de entradas de texto, timeline, UI paciente/psicólogo
3. **Voz:** gravação/upload de áudio e exibição; transcrição (Web Speech API) como incremento
4. **Testes:** catálogo, motor de aplicação, respostas e resultados básicos
5. **Vocacionais:** tipos/configurações específicas e relatórios
6. **Qualidade:** testes automatizados, observabilidade, hardening de segurança e performance
7. **Administração e LGPD (Evolução 10):** painel administrativo (usuários, catálogo, auditoria), consentimentos, portabilidade e solicitações de titulares
8. **Agenda e transferência (Evolução 11):** agendamentos com duração/Modalidade/conflito, isolamento por psicólogo e transferência de vínculo com reatribuição da agenda futura

### 3.5 Administração e privacidade (Evolução 10)
- Consentimentos versionados por titular (`consents`) com registro em `audit_logs`.
- Portabilidade via `GET /api/privacy/export` (JSON com diário, anexos, comentários, testes e consentimentos).
- Solicitações de titulares (`privacy_requests`) com status `PENDING`/`APPROVED`/`REJECTED` gerenciadas pelo administrador.
- Endpoints `/api/admin/**` protegidos por `ROLE_ADMIN`, com proteção contra rebaixamento do último administrador ativo.
- CORS e segredo JWT passam a ser configuráveis por variáveis de ambiente (`APP_CORS_ALLOWED_ORIGIN_PATTERNS`, `JWT_SECRET`).

### 3.6 Agenda e transferência (Evolução 11)
- `appointments` (paciente, psicólogo, início, fim, modalidade, status, título/observações) com índice por profissional e paciente.
- Consultas sempre filtram pelo titular: psicólogo só vê a própria agenda; paciente só vê a própria; administrador pode consultar com filtros.
- Conflito de horário é verificado no backend (`findOverlaps`), desconsiderando agendamentos cancelados.
- Transferência (`patient_links.status = TRANSFERRED`) reativa/cria vínculo com o destino e reatribui somente agendamentos futuros `SCHEDULED`/`CONFIRMED`; histórico clínico permanece intacto.
- Toda transferência gera evento `PATIENT_TRANSFERRED` em `audit_logs` com ator, motivo e participantes.
