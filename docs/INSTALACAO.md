# Instalação e uso do Almox TI

Guia passo a passo para baixar e rodar o **Almox TI** — o sistema de almoxarifado de TI e
manutenção. Não é preciso saber programar: escolha uma das três opções abaixo.

O sistema roda no seu próprio computador, abre no navegador em
**http://localhost:8080** e guarda os dados em uma pasta local. Nada é enviado para a internet.

---

## 1. O que você precisa

- **Java 21** (ou mais novo).
- Um navegador (Chrome, Edge, Firefox...).
- Cerca de 100 MB de espaço livre. Pela opção do código-fonte, reserve mais (~1 GB), porque o
  Maven baixa as dependências de compilação.

### Instalar o Java 21

Baixe o **Eclipse Temurin 21** no site oficial:

<https://adoptium.net/temurin/releases/?version=21>

No Windows, escolha o instalador `.msi` e, durante a instalação, **marque** a opção
**"Set JAVA_HOME variable"**. No macOS use o `.pkg`; no Linux, o `.tar.gz` ou o pacote da
sua distribuição.

### Conferir se o Java está certo

Abra o **Prompt de Comando** (Windows) ou o **Terminal** (macOS/Linux) e digite:

```
java -version
```

A resposta deve mostrar a versão 21 ou maior, por exemplo:

```
openjdk version "21.0.13" 2026-10-20 LTS
OpenJDK Runtime Environment Temurin-21.0.13+9 (build 21.0.13+9-LTS)
```

Se aparecer algo como `java não é reconhecido`, o Java não está instalado — ou foi instalado
sem marcar a opção que coloca o `java` no PATH. Reinstale marcando essa opção.

---

## 2. Opção 1 — Só o arquivo executável (a mais simples)

Não precisa baixar o código nem instalar nada além do Java.

1. Baixe o arquivo **`almox-ti.jar`** na página de releases:

   <https://github.com/twkryan/spring-boot-almoxarifado/releases/latest>

2. Crie uma pasta só para o sistema, por exemplo `C:\AlmoxTI` (ou `~/AlmoxTI`), e coloque o
   `almox-ti.jar` dentro dela. **Não** coloque na pasta Downloads, para não misturar com
   outros arquivos depois.

3. Inicie de uma destas duas formas:

   **Pelo terminal** — abra o terminal **dentro dessa pasta** e rode:

   ```
   java -jar almox-ti.jar
   ```

   No Windows, um atalho: clique com o botão direito na pasta, escolha *Abrir no Terminal*.

   **Com dois cliques (Windows)** — baixe também o arquivo
   [`iniciar.bat`](https://github.com/twkryan/spring-boot-almoxarifado/blob/main/iniciar.bat)
   (botão *Download raw file*) e coloque-o na **mesma pasta** do `almox-ti.jar`. Depois é só dar
   dois cliques nele. O `iniciar.bat` confere se o Java está instalado, avisa se a versão for
   antiga e, quando existe um `almox-ti.jar` ao lado, inicia esse arquivo.

4. Abra o navegador em <http://localhost:8080>.

5. Para **encerrar**, feche a janela do terminal ou pressione `Ctrl+C` nela.

> No macOS/Linux o equivalente é o [`iniciar.sh`](https://github.com/twkryan/spring-boot-almoxarifado/blob/main/iniciar.sh):
> salve na mesma pasta do jar, dê permissão com `chmod +x iniciar.sh` e execute `./iniciar.sh`.

---

## 3. Opção 2 — Docker

Para quem já usa Docker. O comando abaixo cria uma imagem a partir do código-fonte e guarda os
dados em um volume, para não se perderem quando o contêiner for recriado.

```
git clone https://github.com/twkryan/spring-boot-almoxarifado.git
cd spring-boot-almoxarifado
docker build -t almox-ti .
docker run -p 8080:8080 -v almox-dados:/app/data almox-ti
```

- `-p 8080:8080` publica a porta do sistema no seu computador.
- `-v almox-dados:/app/data` guarda o banco em um volume chamado `almox-dados`.
- Se quiser parar: `docker stop <nome-ou-id>` (veja com `docker ps`). Para subir de novo, repita
  o `docker run`.

Se preferir os dados em uma pasta comum em vez de um volume gerenciado pelo Docker, troque
`-v almox-dados:/app/data` por `-v "C:\AlmoxTI\data:/app/data"` (Windows) ou
`-v ~/AlmoxTI/data:/app/data` (macOS/Linux).

Depois acesse <http://localhost:8080>.

---

## 4. Opção 3 — Pelo código-fonte (Java + Maven Wrapper)

Para desenvolvimento ou para quem prefere não usar o jar pronto. Não é preciso instalar o
Maven: o projeto traz o *Maven Wrapper* (`mvnw` no Linux/macOS, `mvnw.cmd` no Windows), que baixa
o Maven sozinho na primeira execução.

```
git clone https://github.com/twkryan/spring-boot-almoxarifado.git
cd spring-boot-almoxarifado
```

**Windows:** dois cliques em `iniciar.bat` — ou, no terminal:

```
mvnw.cmd spring-boot:run
```

**macOS/Linux:**

```
chmod +x mvnw iniciar.sh
./iniciar.sh
```

ou, sem o script:

```
./mvnw spring-boot:run
```

Na primeira vez o Maven baixa as dependências e a compilação demora alguns minutos. Quando
aparecer uma linha parecida com `Started AlmoxarifadoApplication`, acesse
<http://localhost:8080>.

Para gerar o arquivo executável (o mesmo publicado nas releases):

```
mvnw.cmd -DskipTests package        # Windows
./mvnw -DskipTests package          # macOS/Linux
```

O resultado fica em `target/almox-ti.jar`.

---

## 5. Primeiro acesso

O sistema começa **vazio**: não há usuários nem dados de demonstração.

1. Acesse <http://localhost:8080> — você será levado à tela de login.
2. Clique em **Criar conta** (`/cadastro`).
3. Preencha nome, e-mail e senha. **O primeiro usuário cadastrado vira administrador**
   automaticamente; todos os cadastrados depois entram como operadores. Essa distinção só
   identifica quem é o administrador na tela **Usuários** — não restringe telas.
   Contas adicionais podem ser criadas ali por qualquer usuário logado.
4. Já logado, cadastre a estrutura básica, nesta ordem:
   1. **Setores** — as áreas que recebem os itens.
   2. **Técnicos** — as pessoas que retiram e repõem peças.
   3. **Itens** — o catálogo de peças e materiais, com quantidade em estoque.
5. Depois disso o dia a dia acontece em **Retiradas**, **Reposições**, **Empréstimos**,
   **Histórico** e **Relatórios**.

> Guarde bem o e-mail e a senha do administrador. Não existe recuperação de senha por e-mail.

---

## 6. Onde ficam os dados, backup e como zerar

O banco é um arquivo H2 criado na pasta **`data/`**, relativa à pasta de onde o sistema roda:

| Como você iniciou | Onde ficam os dados |
|---|---|
| `java -jar almox-ti.jar` em `C:\AlmoxTI` | `C:\AlmoxTI\data\almoxarifado.mv.db` |
| `iniciar.bat` / `iniciar.sh` na pasta do projeto | `data/almoxarifado.mv.db` na pasta do projeto |
| Docker com `-v almox-dados:/app/data` | no volume `almox-dados` (gerenciado pelo Docker) |

**Faça backup com o sistema parado** (ou pelo menos sem ninguém usando), copiando a pasta `data`
inteira para outro lugar:

```
copy data\almoxarifado.mv.db D:\backup\almoxarifado-2026-11-19.mv.db     (Windows)
cp data/almoxarifado.mv.db ~/backup/almoxarifado-2026-11-19.mv.db        (macOS/Linux)
```

Para **restaurar**, pare o sistema, devolva o arquivo `.mv.db` para a pasta `data/` com o nome
original e inicie novamente.

Para **zerar tudo** (recomeçar do zero, apagando contas e cadastros):

1. Pare o sistema.
2. Apague a pasta `data` inteira (ou só os arquivos `almoxarifado.*.db`).
3. Inicie de novo — um banco vazio é criado e você faz o primeiro cadastro outra vez.

No Docker: `docker volume rm almox-dados` (com o contêiner parado) recria o banco do zero.

Os arquivos `data/almoxarifado.lock.db` e `data/almoxarifado.trace.db`, quando existirem, são
auxiliares do H2 e podem ser copiados/apagados junto com o `.mv.db`.

---

## 7. Acessar de outro computador da mesma rede

O sistema atende na porta 8080 de qualquer computador que consiga chegar até a máquina onde ele
roda.

1. Descubra o IP da máquina que roda o sistema:

   ```
   ipconfig        (Windows — veja "Endereço IPv4", algo como 192.168.0.15)
   ip addr / ifconfig   (Linux/macOS)
   ```

2. No outro computador, abra `http://192.168.0.15:8080` (troque pelo IP encontrado).

3. Se não abrir, libere a porta no firewall da máquina que roda o sistema.
   **Windows** (Prompt de Comando como administrador):

   ```
   netsh advfirewall firewall add rule name="Almox TI" dir=in action=allow protocol=TCP localport=8080
   ```

   **Linux com ufw:** `sudo ufw allow 8080/tcp`.
   No macOS, aceite o aviso de "aceitar conexões de entrada" que aparece na primeira execução.

> Aviso: o sistema usa HTTP simples, sem criptografia. Use na rede interna da empresa e não o
> exponha diretamente na internet.

---

## 8. Problemas comuns

**`java não é reconhecido` / `'java' não é reconhecido como um comando interno`**
O Java não está instalado ou não está no PATH. Instale o Temurin 21 marcando a opção
"Set JAVA_HOME variable" e abra um **novo** terminal (o antigo mantém o PATH anterior).

**O sistema não sobe e aparece `Port 8080 was already in use`**
Outro programa (ou outra cópia do Almox TI) já está usando a porta 8080. Rode em outra porta:

```
java -jar almox-ti.jar --server.port=9090
mvnw.cmd -Dspring-boot.run.arguments=--server.port=9090 spring-boot:run
```

e acesse <http://localhost:9090>. No Docker, mude o mapeamento: `-p 9090:8080`.

**`UnsupportedClassVersionError` ou erro dizendo que a versão da classe não é suportada**
O Java em uso é mais antigo que o 21. Rode `java -version` para confirmar e instale o 21.

**A tela não carrega e o terminal mostra `Started AlmoxarifadoApplication` mas nada abre**
Confirme o endereço `http://localhost:8080` (com `http`, não `https`) e que o terminal continua
aberto — fechar o terminal encerra o sistema.

**Esqueci a senha**
Não há recuperação nem troca de senha pela interface: as senhas ficam guardadas com hash e não
existe tela para redefini-las. Se ainda houver outro login funcionando, use-o normalmente
(qualquer usuário logado cria novas contas em **Usuários**). Se não houver, a única saída é zerar
o banco (seção 6) e refazer o primeiro cadastro — o que apaga os dados, então tenha um backup.

**No Linux/macOS: `Permission denied` ao rodar `./iniciar.sh` ou `./mvnw`**
Dê permissão de execução: `chmod +x iniciar.sh mvnw`.

**No Docker os dados desaparecem ao recriar o contêiner**
Faltou o volume: use `-v almox-dados:/app/data` no `docker run`.

---

## 9. Para quem vai manter o projeto

- Código: <https://github.com/twkryan/spring-boot-almoxarifado>
- Integração contínua: cada push/PR para `main` compila e roda os testes
  (`.github/workflows/ci.yml`).
- Releases: uma tag `v*` gera o `almox-ti.jar` e publica na página de releases
  (`.github/workflows/release.yml`).
- Requisitos de build: Java 21 e `pom.xml` com `finalName` `almox-ti`, o que produz
  `target/almox-ti.jar`.
