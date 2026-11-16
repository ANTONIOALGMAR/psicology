# Operação e Deploy - PsiCare

## Desenvolvimento local
- Banco: `docker compose up -d postgres`
- Backend: `cd backend && mvn spring-boot:run` (http://localhost:8080)
- Frontend: `cd frontend && npm install && npm run dev` (http://localhost:5173)

## Execução completa com Docker
1. Copie `.env.example` para `.env` e ajuste segredos/senhas (recomendado fora de desenvolvimento).
2. Execute `docker compose up --build -d` (ou `./iniciar.sh` / `iniciar.bat`).
3. Acesse http://localhost:3000. API: http://localhost:8080, saúde: `/actuator/health`.

Para atualizar uma instalação existente feita pelos scripts automáticos, execute novamente `./instalar-e-rodar.sh` ou `instalar-e-rodar.bat`.

## Variáveis de ambiente importantes
- `JWT_SECRET`: segredo de assinatura JWT (obrigatório alterar em produção; mínimo 32 bytes).
- `SPRING_DATASOURCE_*` / `POSTGRES_*`: credenciais e URL do banco.
- `APP_CORS_ALLOWED_ORIGIN_PATTERNS`: origens CORS permitidas (ex.: domínio de produção).
- `APP_STORAGE_UPLOAD_DIR`: diretório de uploads (no Docker, persistido em volume `uploads`).

## Produção (recomendações)
- **HTTPS/TLS:** publicar atrás de proxy reverso (Nginx/Traefik/Caddy) com certificado válido.
- **Segredos:** não usar valores padrão; rotacionar `JWT_SECRET` e senhas; alterar senha do admin padrão (`admin@psicare.com`).
- **Banco:** backups regulares do volume/dados PostgreSQL e testes de restauração; definir política de retenção.
- **Privacidade/LGPD:** consentimento, minimização de dados, controle de acesso, trilha de auditoria (tabela `audit_logs`) e procedimentos de atendimento a titulares.
- **CORS:** restringir `APP_CORS_ALLOWED_ORIGIN_PATTERNS` ao(s) domínio(s) reais.
- **Uploads:** definir limites, antivírus/varredura conforme política e armazenamento seguro.
- **Observabilidade:** monitorar `/actuator/health` (detalhes desabilitados por padrão), logs de aplicação e alertas.

## Validação e CI
- **Frontend:** `cd frontend && npm ci && npm test && npm run build`
- **Backend:** `cd backend && mvn test` (testes de integração com H2/Flyway em perfil de teste)
- **CI:** workflow em `.github/workflows/ci.yml` executa frontend e backend em push/PR para `main`.

## Backup e restauração (Docker, exemplo)
- Backup do banco: `docker compose exec -T db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" > backup.sql`
- Restauração: `cat backup.sql | docker compose exec -T db psql -U "$POSTGRES_USER" "$POSTGRES_DB"`
- Uploads: backup do volume `uploads` conforme estratégia de infraestrutura.

> Ajuste comandos/usuários/nomes conforme seu `.env` e ambiente.
