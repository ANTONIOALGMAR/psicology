# Como usar o PsiCare (guia simples)

O PsiCare é um sistema de acompanhamento psicológico com diário (texto e áudio), testes psicológicos/vocacionais, vínculo paciente–psicólogo e área de conta/segurança.

## Acesso rápido (após iniciar)
- **Sistema:** http://localhost:3000
- **API (saúde):** http://localhost:8080/actuator/health
- **Administrador padrão (único, criado automaticamente):**
  - E-mail: `admin@psicare.com`
  - Senha: `Admin@123`
  - **Importante:** altere a senha após o primeiro acesso em **Conta → Alterar senha** (especialmente em produção).

## Opção mais fácil (automática): 1 arquivo para instalar e rodar
Os scripts abaixo tentam: **instalar/iniciar o Docker (quando possível)**, baixar o sistema do GitHub e executá-lo.

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

Por padrão o sistema é baixado em `~/PsiCare` (Linux/Mac) ou `%USERPROFILE%\PsiCare` (Windows). Você pode mudar com a variável `PSICARE_DIR`.

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

## Como usar (passo a passo)
1. **Entrar:** acesse http://localhost:3000 e faça login (ou cadastre-se).
2. **Paciente:** use **Meu Diário** para registrar texto/áudio e acompanhar feedback; use **Testes** para responder instrumentos; use **Conta** para alterar senha/perfil.
3. **Psicólogo:** acesse **Pacientes** para vincular por e-mail e acompanhar diários/áudios, enviando feedback; use **Conta** para gerenciar seu perfil/senha.
4. **Admin:** entre com a conta padrão e, se necessário, crie convites/perfis conforme a operação.

## Como parar e iniciar novamente
- **Parar:** `parar.bat` (Windows) ou `./parar.sh` (Linux/Mac); ou `docker compose down` na pasta.
- **Iniciar novamente:** `iniciar.bat` / `./iniciar.sh` (ou `docker compose up -d`).

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
