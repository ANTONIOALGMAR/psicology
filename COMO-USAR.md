# Como usar o PsiCare (guia simples)

Este guia é para quem quer **baixar e usar o sistema** com o mínimo de passos.

## Opção mais fácil (automática): 1 arquivo para instalar e rodar
Os scripts abaixo tentam fazer tudo automaticamente: **instalar/iniciar o Docker (quando possível)**, baixar o sistema do GitHub e executá-lo.

### Windows
1. Baixe o arquivo: [instalar-e-rodar.bat](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat) (se precisar: clique com o botão direito → "Salvar link como...")
2. Dê **dois cliques** em `instalar-e-rodar.bat`
3. Se o Docker não estiver instalado, o script tentará instalá-lo (via `winget` ou instalador do Docker Desktop). **Pode ser necessário concluir a instalação, reiniciar o computador e/ou abrir o Docker Desktop e executar o arquivo novamente.**
4. Ao final, o sistema abrirá em: **http://localhost:3000**

### Linux/Mac
1. Baixe o arquivo: [instalar-e-rodar.sh](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh)
2. No terminal, execute:
   ```bash
   chmod +x instalar-e-rodar.sh
   ./instalar-e-rodar.sh
   ```
3. Se o Docker não estiver instalado, o script tentará instalá-lo (Linux: script oficial do Docker, podendo pedir senha `sudo`; Mac: via Homebrew, se disponível). **Pode ser necessário reiniciar a sessão/computador, abrir o Docker Desktop (Mac) e executar o script novamente.**

Por padrão o sistema é baixado em `~/PsiCare` (Linux/Mac) ou `%USERPROFILE%\PsiCare` (Windows). Você pode mudar a pasta definindo a variável `PSICARE_DIR` antes de executar.

> **Observação:** a instalação do Docker pode exigir permissões de administrador, aceite de termos e, em alguns casos, reinicialização. Se algo não concluir automaticamente, instale o Docker Desktop manualmente em https://www.docker.com/products/docker-desktop/ e execute o script novamente.

## Opção alternativa: baixar o ZIP do projeto
1. Acesse: https://github.com/ANTONIOALGMAR/psicology
2. Clique em **Code → Download ZIP** e extraia a pasta.
3. Com o Docker Desktop aberto, execute:
   - **Windows:** `iniciar.bat`
   - **Linux/Mac:** `./iniciar.sh`
4. Abra: **http://localhost:3000**

## Como usar o sistema
- **Criar conta:** clique em "Cadastre-se" e preencha seus dados (cadastro aberto).
- **Convite (opcional):** se você recebeu um código, marque "Tenho um código de convite" no cadastro.
- **Diário:** registre anotações por texto e, se o navegador permitir, use o ditado por voz.
- **Testes:** acesse a área de testes psicológicos e vocacionais pelo menu.

## Como parar e iniciar novamente
- **Parar:** execute `parar.bat` (Windows) ou `./parar.sh` (Linux/Mac) dentro da pasta do sistema; ou rode `docker compose down` na pasta.
- **Iniciar novamente:** execute `iniciar.bat` / `./iniciar.sh` (ou `docker compose up -d`).

## Problemas comuns
- **"Docker não encontrado" / "Docker não está em execução":** instale o Docker Desktop e abra-o antes de iniciar.
- **Demora na primeira vez:** é normal, pois o sistema está sendo preparado/baixado.
- **Página não abre:** aguarde um pouco mais e atualize `http://localhost:3000`. Verifique se o Docker Desktop está ativo.
- **Porta em uso (3000 ou 8080):** feche outros programas que usem essas portas ou ajuste no arquivo `docker-compose.yml`.

## Para enviar para outra pessoa
Envie o link do instalador automático:
- Windows: https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat
- Linux/Mac: https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh

E peça para instalar/abrir o **Docker Desktop** e executar o arquivo baixado.
