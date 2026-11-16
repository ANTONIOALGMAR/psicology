# Como usar o PsiCare (guia simples)

Este guia é para quem quer **baixar e usar o sistema** com o mínimo de passos.

## O que você precisa instalar (apenas 1 coisa)
- **Docker Desktop** (programa gratuito que permite rodar o sistema):
  - Windows/Mac: https://www.docker.com/products/docker-desktop/
  - Depois de instalar, **abra o Docker Desktop** e espere ele ficar pronto (ícone ativo).

> Você não precisa instalar Java, Node ou banco de dados separadamente — o Docker cuida disso.

## Como baixar o sistema
1. Acesse: https://github.com/ANTONIOALGMAR/psicology
2. Clique no botão verde **"Code"** (Código) e depois em **"Download ZIP"**.
3. Extraia (descompacte) o arquivo ZIP em uma pasta no seu computador (ex.: Área de Trabalho).

## Como iniciar
1. Abra a pasta do sistema que você extraiu.
2. Dê dois cliques no arquivo de início:
   - **Windows:** `iniciar.bat`
   - **Linux/Mac:** `iniciar.sh` (se necessário, permita execução: `chmod +x iniciar.sh` e depois execute `./iniciar.sh`)
3. Aguarde alguns minutos na **primeira vez** (o sistema será preparado automaticamente).
4. Quando terminar, abra o navegador em: **http://localhost:3000**
   - No Windows, o `iniciar.bat` tenta abrir o navegador automaticamente.

## Como usar
- **Criar conta:** clique em "Cadastre-se" e preencha seus dados (cadastro aberto).
- **Convite (opcional):** se você recebeu um código, marque "Tenho um código de convite" no cadastro.
- **Diário:** registre anotações por texto e, se o navegador permitir, use o ditado por voz.
- **Testes:** acesse a área de testes psicológicos e vocacionais pelo menu.

## Como parar o sistema
- **Windows:** dê dois cliques em `parar.bat`
- **Linux/Mac:** execute `./parar.sh`

(Se preferir, você também pode parar pelo Docker Desktop.)

## Problemas comuns
- **"Docker não encontrado"**: instale o Docker Desktop e abra-o antes de iniciar.
- **Demora na primeira vez**: é normal, pois o sistema está sendo preparado.
- **Página não abre**: aguarde um pouco mais e tente atualizar `http://localhost:3000`. Verifique se o Docker Desktop está em execução.
- **Porta em uso**: feche outros programas que possam usar a porta 3000 ou 8080, ou ajuste as portas no arquivo `docker-compose.yml`.

## Para enviar para outra pessoa
Basta compartilhar este link para download: https://github.com/ANTONIOALGMAR/psicology (botão **Code > Download ZIP**) e orientar a pessoa a seguir este guia (instalar Docker Desktop e executar `iniciar.bat`/`iniciar.sh`).
