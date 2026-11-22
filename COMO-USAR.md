# Como usar o PsiCare (guia simples)

O PsiCare é um sistema de acompanhamento psicológico com diário (texto e áudio), testes psicológicos/vocacionais, vínculo paciente–psicólogo, agenda de atendimentos, transferência de clientes, administração/LGPD e orientador automático para momentos fora do horário. Funciona igual no computador, Android e iOS pelo navegador (ou instalado como app).

## Acesso rápido (após iniciar)
- **Sistema:** http://localhost:3000
- **API (saúde):** http://localhost:8080/actuator/health
- **Administrador padrão (único, criado automaticamente):**
  - E-mail: `admin@psicare.com`
  - Senha: `Admin@123`
  - **Importante:** altere a senha após o primeiro acesso em **Conta → Alterar senha** (especialmente em produção).

## Opção mais fácil (automática): 1 arquivo para instalar, atualizar e rodar
Os scripts abaixo tentam: **instalar/iniciar o Docker (quando possível)**, baixar o sistema do GitHub e executá-lo. **Se o sistema já estiver baixado** na pasta de instalação, ao executar o mesmo script novamente ele **baixa a atualização mais recente** e reinicia automaticamente.

### Windows
1. Baixe: [instalar-e-rodar.bat](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat) (clique direito → "Salvar link como...", se necessário)
2. Dê **dois cliques** em `instalar-e-rodar.bat`
3. Se o Docker não estiver instalado, o script tentará instalá-lo. Pode ser necessário concluir a instalação, reiniciar o computador e/ou abrir o Docker Desktop e executar novamente.
4. Abra: **http://localhost:3000**

### Linux/Mac
1. Baixe: [instalar-e-rodar.sh](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh)
2. No terminal:
   ```bash
   chmod +x instalar-e-rodar.sh
   ./instalar-e-rodar.sh
   ```
3. Se o Docker não estiver instalado, o script tentará instalá-lo (Linux: script oficial do Docker, podendo pedir `sudo`; Mac: via Homebrew, se disponível). Pode ser necessário reiniciar, abrir o Docker Desktop (Mac) e executar novamente.

Por padrão o sistema é baixado em `~/PsiCare` (Linux/Mac) ou `%USERPROFILE%\PsiCare` (Windows). Você pode mudar com a variável `PSICARE_DIR`. Para atualizar no futuro, basta executar o mesmo arquivo novamente.

> **Observação:** instalação do Docker pode exigir permissões de administrador e reinicialização. Se não concluir automaticamente, instale o Docker Desktop em https://www.docker.com/products/docker-desktop/ e execute o script novamente.

## Opção alternativa: baixar o ZIP do projeto
1. Acesse: https://github.com/ANTONIOALGMAR/psicology
2. **Code → Download ZIP**, extraia a pasta.
3. Com o Docker Desktop aberto, execute:
   - **Windows:** `iniciar.bat`
   - **Linux/Mac:** `./iniciar.sh`
4. Abra: **http://localhost:3000**

## Funcionalidades
- **Cadastro e login:** cadastro aberto; suporte opcional a código de convite. Perfis: Paciente, Psicólogo(a) e Administrador.
- **Diário completo (paciente):** registros por texto, ditado por voz (quando suportado pelo navegador), gravação/upload de áudio, reprodução e remoção de anexos, busca e filtro por humor, organização por mês. Psicólogos vinculados podem visualizar e comentar.
- **Painel do psicólogo e vínculo:** psicólogos podem vincular pacientes (por e-mail de paciente já cadastrado) e visualizar diários/evolução, além de registrar feedback nos registros. Convites criados por psicólogos podem vincular automaticamente ao cadastro do paciente.
- **Testes psicológicos e vocacionais:** motor completo de testes com questões, opções, salvar progresso, finalizar tentativa, pontuação/resumo e histórico de tentativas.
- **Conta e segurança:** edição de perfil (nome) e alteração de senha.
- **Agenda e transferência:** agendamentos com data/hora/duração e bloqueio de sobreposição; transferência de paciente com reatribuição da agenda futura e registro em auditoria.
- **Administração e LGPD:** painel administrativo, consentimentos, exportação dos dados do titular, solicitações de cópia/exclusão e trilha de auditoria.
- **Orientador automático:** botão **💬 Preciso de ajuda** para pacientes; o psicólogo configura sua disponibilidade e recebe alertas de crise (sem o conteúdo da mensagem). Em crise: 188 (CVV), 192 (SAMU), 190 ou pronto-socorro.

## Como usar (passo a passo)
1. **Entrar:** acesse http://localhost:3000 e faça login (ou cadastre-se).
2. **Paciente:** use **Meu Diário** para registrar texto/áudio e acompanhar feedback; use **Testes** para responder instrumentos; use **Conta** para alterar senha/perfil.
3. **Psicólogo:** acesse **Pacientes** para vincular por e-mail e acompanhar diários/áudios, enviando feedback; use **Agenda** para marcar atendimentos, **Disponibilidade** para informar seu horário e **Pacientes → Alertas de crise** para acompanhar sinais de risco.
4. **Paciente (ajuda):** aceite a política em **Privacidade** e toque em **💬 Preciso de ajuda** quando precisar de orientação fora do horário do profissional.
5. **Admin:** entre com a conta padrão, crie/perfis, acompanhe LGPD, auditoria e o histórico de conversas em **Administração**.

## Computador, Android e iOS
Use o mesmo endereço e a mesma conta em qualquer aparelho. As telas se ajustam sozinhas (incluindo a área segura do iPhone) e as mesmas regras de acesso valem para pacientes e psicólogos.
- **Android (Chrome):** menu ⋮ → **Instalar aplicativo** ou **Adicionar à tela inicial**.
- **iPhone/iPad (Safari):** botão de compartilhar → **Adicionar à Tela de Início**.
- O cache do app guarda só telas e ícones; mensagens e dados clínicos nunca ficam salvos no aparelho pelo service worker.

## Como parar, iniciar e atualizar
- **Parar:** `parar.bat` (Windows) ou `./parar.sh` (Linux/Mac); ou `docker compose down` na pasta.
- **Iniciar novamente:** `iniciar.bat` / `./iniciar.sh` (ou `docker compose up -d`).
- **Atualizar para a versão mais recente:** execute novamente `instalar-e-rodar.bat` / `./instalar-e-rodar.sh` (ele detecta a instalação existente, baixa a atualização e reinicia o sistema).

## Problemas comuns
- **Docker não encontrado / não está em execução:** instale e abra o Docker Desktop antes de iniciar.
- **Demora na primeira vez:** normal (download/build das imagens).
- **Página não abre:** aguarde, atualize http://localhost:3000 e verifique se os containers estão ativos (`docker compose ps`).
- **Porta em uso (3000 ou 8080):** feche outros programas nessas portas ou ajuste `docker-compose.yml`.
- **Áudio/ditado:** ditado por voz depende do suporte do navegador; gravação de áudio requer permissão de microfone.

## Observações de segurança e privacidade
- Dados sensíveis: use em ambiente confiável, mantenha senhas seguras e altere a senha padrão do administrador.
- Os resultados de testes são instrumentos de apoio e devem ser interpretados por profissional qualificado.
- Para uso em produção, configure HTTPS, segredos fortes (ex.: `JWT_SECRET`), backups e políticas adequadas de privacidade.

## Para enviar para outra pessoa
Envie o instalador automático:
- Windows: https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat
- Linux/Mac: https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh
