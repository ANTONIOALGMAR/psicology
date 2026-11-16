# Como usar o PsiCare (guia simples)

Este guia é para quem quer **baixar e usar o sistema** com o mínimo de passos.

## Opção mais fácil (automática): baixar e rodar com 1 arquivo
> Requer apenas o **Docker Desktop** instalado e aberto.

### 1) Instale o Docker Desktop (uma vez)
- Download: https://www.docker.com/products/docker-desktop/
- Instale e **abra o Docker Desktop**. Aguarde até ele indicar que está em execução.

### 2) Baixe e execute o instalador automático
- **Windows:**
  1. Baixe o arquivo: [instalar-e-rodar.bat](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.bat) (clique com o botão direito → "Salvar link como..." se necessário)
  2. Dê **dois cliques** no arquivo `instalar-e-rodar.bat`
  3. O script vai baixar o sistema do GitHub, preparar e iniciar automaticamente
- **Linux/Mac:**
  1. Baixe o arquivo: [instalar-e-rodar.sh](https://raw.githubusercontent.com/ANTONIOALGMAR/psicology/main/instalar-e-rodar.sh)
  2. No terminal, execute:
     ```bash
     chmod +x instalar-e-rodar.sh
     ./instalar-e-rodar.sh
     ```

Ao final, o sistema abrirá em: **http://localhost:3000**  
(Por padrão o sistema é baixado em `~/PsiCare` ou `%USERPROFILE%\PsiCare`.)

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
